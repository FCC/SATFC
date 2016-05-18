/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.math.util.FastMath;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.polling.IPollingService;
import ca.ubc.cs.beta.stationpacking.polling.ProblemIncrementor;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ISATFCInterruptible;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 01/03/15.
 * Abstracts away the Containment Cache data structure, which is really being accessed using web requests
 * Not threadsafe!
 */
@Slf4j
public class ContainmentCacheProxy implements ICacher, ISATFCInterruptible {

    // if the text is smaller than this length in bytes, then compression probably isn't worth the trouble
    public static final int MIN_GZIP_LENGTH = 860;
    public static Date lastSuccessfulCommunication;

    private final CacheCoordinate coordinate;
    private final CloseableHttpAsyncClient httpClient;
    private final String SAT_URL;
    private final String UNSAT_URL;
    private final String CACHE_URL;
    private final AtomicReference<Future<HttpResponse>> activeFuture;
    private final int numAttempts;
    private final boolean noErrorOnServerUnavailable;
    private final ProblemIncrementor problemIncrementor;

    public ContainmentCacheProxy(@NonNull String baseServerURL, @NonNull CacheCoordinate coordinate, int numAttempts, boolean noErrorOnServerUnavailable, IPollingService pollingService, @NonNull CloseableHttpAsyncClient httpClient) {
        this.httpClient = httpClient;
        SAT_URL = baseServerURL + "/v1/cache/query/SAT";
        UNSAT_URL = baseServerURL + "/v1/cache/query/UNSAT";
        CACHE_URL = baseServerURL + "/v1/cache";
        this.coordinate = coordinate;
        activeFuture = new AtomicReference<>();
        this.numAttempts = numAttempts;
        this.noErrorOnServerUnavailable = noErrorOnServerUnavailable;
        problemIncrementor = new ProblemIncrementor(pollingService, this);
    }

    /**
     * Object used to represent a cache lookup request
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContainmentCacheRequest {

        public ContainmentCacheRequest(StationPackingInstance instance, CacheCoordinate coordinate) {
            this.instance = instance;
            this.coordinate = coordinate;
        }

        private StationPackingInstance instance;
        private CacheCoordinate coordinate;
        private SolverResult result;
    }

    public ContainmentCacheSATResult proveSATBySuperset(StationPackingInstance instance, ITerminationCriterion terminationCriterion) {
        try {
            problemIncrementor.scheduleTermination(terminationCriterion);
            return makePost(SAT_URL, new ContainmentCacheRequest(instance, coordinate), ContainmentCacheSATResult.class, ContainmentCacheSATResult.failure(), terminationCriterion, numAttempts);
        } finally {
            problemIncrementor.jobDone();
        }
    }

    public ContainmentCacheUNSATResult proveUNSATBySubset(StationPackingInstance instance, ITerminationCriterion terminationCriterion) {
        try {
            problemIncrementor.scheduleTermination(terminationCriterion);
            return makePost(UNSAT_URL, new ContainmentCacheRequest(instance, coordinate), ContainmentCacheUNSATResult.class, ContainmentCacheUNSATResult.failure(), terminationCriterion, numAttempts);
        } finally {
            problemIncrementor.jobDone();
        }
    }

    @Override
    public void cacheResult(StationPackingInstance instance, SolverResult result, ITerminationCriterion terminationCriterion) {
        makePost(CACHE_URL, new ContainmentCacheRequest(instance, coordinate, result), null, null, terminationCriterion, numAttempts);
    }

    private <T> T makePost(String URL, ContainmentCacheRequest request, Class<T> responseClass, T failure, ITerminationCriterion terminationCriterion, int remainingAttempts) {
        try {
            return makePost(URL, request, responseClass, failure, terminationCriterion);
        } catch (Exception e) {
            log.error("Error making a web request", e);
            int newRemainingAttempts = remainingAttempts - 1;
            if (newRemainingAttempts > 0) {
                log.error("Retrying web request. Request will be retried {} more time(s)", newRemainingAttempts);
                return makePost(URL, request, responseClass, failure, terminationCriterion, newRemainingAttempts);
            } else {
                log.error("The retry quota for this web request has been exceeded");
                if (noErrorOnServerUnavailable) {
                    log.error("Continuing to solve the problem without the server...");
                    return failure;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private <T> T makePost(String URL, ContainmentCacheRequest request, Class<T> responseClass, T failure, ITerminationCriterion terminationCriterion) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(URL);
        final String uriString = builder.build().toUriString();
        final HttpPost httpPost = new HttpPost(uriString);
        log.debug("Making a request to the cache server for instance " + request.getInstance().getName() + " " + uriString);
        final String jsonRequest = JSONUtils.toString(request);
        // possibly do gzip compression
        if (jsonRequest.length() > MIN_GZIP_LENGTH) {
            final ByteArrayOutputStream arr = new ByteArrayOutputStream();
            try {
                final OutputStream zipper = new GZIPOutputStream(arr);
                zipper.write(jsonRequest.getBytes());
                zipper.close();
            } catch (IOException e) {
                throw new RuntimeException("Error compressing json http post request to gzip", e);
            }
            final ByteArrayEntity postEntity = new ByteArrayEntity(arr.toByteArray());
            postEntity.setContentEncoding("gzip");
            postEntity.setContentType("application/json");
            httpPost.setEntity(postEntity);
        } else {
            httpPost.setEntity(new StringEntity(jsonRequest, ContentType.APPLICATION_JSON));
        }
        httpPost.addHeader("Accept-Encoding", "gzip");
        if (terminationCriterion.hasToStop()) {
            return failure;
        }
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<HttpResponse> httpResponse = new AtomicReference<>();
            final AtomicReference<Exception> exception = new AtomicReference<>();
            activeFuture.set(httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    log.trace("Back from making web request");
                    httpResponse.set(result);
                    lastSuccessfulCommunication = new Date();
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
            boolean timedOut;
            try {
                double waitTime = FastMath.max(0, terminationCriterion.getRemainingTime());
                long waitTimeInMs = (long) (1000 * waitTime);
                timedOut = !latch.await(waitTimeInMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for countdown latch", e);
            }
            if (timedOut) {
                log.debug("Timed out while waiting for server to respond");
                interrupt();
            }
            final Exception ex = exception.get();
            if (ex != null) {
                throw new RuntimeException("Error making web request", ex);
            }
            if (terminationCriterion.hasToStop()) {
                return failure;
            }
            if (responseClass != null) {
                HttpEntity responseEntity = httpResponse.get().getEntity();
                // Check to see if the response is compressed using gzip
                final Header ceheader = responseEntity.getContentEncoding();
                if (ceheader != null && Arrays.stream(ceheader.getElements()).anyMatch(codec -> codec.getName().equalsIgnoreCase("gzip"))) {
                    log.trace("gzip response detected");
                    responseEntity = new GzipDecompressingEntity(responseEntity);
                }
                final String response = EntityUtils.toString(responseEntity);
                return JSONUtils.toObject(response, responseClass);
            } else {
                return null; // Not expecting a response
            }
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

}