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
package ca.ubc.cs.beta.stationpacking.webapp.model;

import java.io.Serializable;
import java.util.List;

public class ErrorResponse implements Serializable {
  private static final long serialVersionUID = 7707914283100541061L;
  
  private Long errorId;
  private List<String> errors;
  
  public ErrorResponse() {
    // empty
  }
  
  public ErrorResponse(Long errorId, List<String> errors) {
    this.errorId = errorId;
    this.errors = errors;
  }

  public Long getErrorId() {
    return errorId;
  }

  public void setErrorId(Long errorId) {
    this.errorId = errorId;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }
}