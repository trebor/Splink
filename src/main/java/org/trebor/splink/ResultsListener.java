package org.trebor.splink;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

public interface ResultsListener
{
  int onTuple(TupleQueryResult result, Splink splink) throws QueryEvaluationException;

  int onGraph(GraphQueryResult result, Splink splink) throws QueryEvaluationException;
  
  boolean onBoolean(boolean result, Splink splink);
}