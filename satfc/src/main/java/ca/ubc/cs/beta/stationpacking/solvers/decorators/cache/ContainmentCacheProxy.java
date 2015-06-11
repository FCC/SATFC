/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.solvers.decorators.cache;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by newmanne on 01/03/15.
 * Abstracts away the Containment Cache data structure, which is really being accessed using web requests
 * Not threadsafe!
 */
@Slf4j
public class ContainmentCacheProxy {

    private final CacheCoordinate coordinate;
    private final static CloseableHttpAsyncClient httpClient;
    private final String SAT_URL;
    private final String UNSAT_URL;
    private final AtomicReference<Future<HttpResponse>> activeFuture;

    static {
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
    }

    public ContainmentCacheProxy(String baseServerURL, CacheCoordinate coordinate) {
        SAT_URL = baseServerURL + "/v1/cache/query/SAT";
        UNSAT_URL = baseServerURL + "/v1/cache/query/UNSAT";
        this.coordinate = coordinate;
        activeFuture = new AtomicReference<>();
    }

    /**
     * Object used to represent a cache lookup request
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContainmentCacheRequest {
        private StationPackingInstance instance;
        private CacheCoordinate coordinate;
    }

    public ContainmentCacheSATResult proveSATBySuperset(StationPackingInstance instance, ITerminationCriterion terminationCriterion) {
        return makePost(SAT_URL, instance, ContainmentCacheSATResult.class, ContainmentCacheSATResult.failure(), terminationCriterion);
    }

    public ContainmentCacheUNSATResult proveUNSATBySubset(StationPackingInstance instance, ITerminationCriterion terminationCriterion) {
        return makePost(UNSAT_URL, instance, ContainmentCacheUNSATResult.class, ContainmentCacheUNSATResult.failure(), terminationCriterion);
    }

    private <T> T makePost(String URL, StationPackingInstance instance, Class<T> responseClass, T failure, ITerminationCriterion terminationCriterion) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(URL);
        final String uriString = builder.build().toUriString();
        final HttpPost httpPost = new HttpPost(uriString);
        log.debug("Making a request to the cache server for instance " + instance.getName() + " " + uriString);
        final ContainmentCacheRequest request = new ContainmentCacheRequest(instance, coordinate);
        final String jsonRequest = JSONUtils.toString(request);
        final StringEntity stringEntity = new StringEntity(jsonRequest, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<HttpResponse> httpResponse = new AtomicReference<>();
            final AtomicReference<Exception> exception = new AtomicReference<>();
            activeFuture.set(httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    log.trace("Back from making web request");
                    httpResponse.set(result);
                    latch.countDown();
                }

                @Override
                public void failed(Exception ex) {
                    exception.set(ex);
                    latch.countDown();
                }

                @Override
                public void cancelled() {
                    log.debug("Web request aborted");
                    latch.countDown();
                }
            }));
            if (terminationCriterion.hasToStop()) {
                return failure;
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for countdown latch", e);
            }
            final Exception ex = exception.get();
            if (ex != null) {
                throw new RuntimeException("Error making web request", ex);
            }
            if (terminationCriterion.hasToStop()) {
                return failure;
            }
            final String response = EntityUtils.toString(httpResponse.get().getEntity());
            return JSONUtils.toObject(response, responseClass);
        } catch (IOException e) {
            throw new RuntimeException("Error reading input stream from httpResponse", e);
        }
    }

    public void interrupt() {
        final Future<HttpResponse> future = activeFuture.getAndSet(null);
        if (future != null) {
            log.debug("Cancelling web request future");
            future.cancel(true);
        }
    }

    public synchronized void notifyShutdown() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't close http client", e);
        }
    }

}