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
package ca.ubc.cs.beta.stationpacking.webapp.rest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.ubc.cs.beta.stationpacking.webapp.model.ErrorResponse;

@Slf4j
public abstract class AbstractController {

  protected final static String JSON_CONTENT = "application/json";

  @ExceptionHandler
  @RequestMapping(produces = JSON_CONTENT)
  @ResponseBody
  ErrorResponse handleDefaultException(HttpServletResponse response, Exception ex) {
    log.error("Unexpected error while performing REST action.", ex);

    log.debug("Default exception handling - setting SC_BAD_REQUEST");
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

    final List<String> errors = new ArrayList<String>();
    handleException(ex, errors);
    return new ErrorResponse(23423l, errors);
  }

  private void handleException(Throwable ex, List<String> errors) {
    if (ex.getCause() != null) {
      handleException(ex.getCause(), errors);
    }

    errors.add(ex.getMessage());
  }
}