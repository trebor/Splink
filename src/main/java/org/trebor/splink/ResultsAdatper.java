package org.trebor.splink;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

public class ResultsAdatper implements ResultsListener
{
  public int onTuple(TupleQueryResult result, Splink splink)
    throws QueryEvaluationException
  {
    throw new UnsupportedOperationException();
  }

  public int onGraph(GraphQueryResult result, Splink splink)
    throws QueryEvaluationException
  {
    throw new UnsupportedOperationException();
  }

  public boolean onBoolean(boolean result, Splink splink)
  {
    throw new UnsupportedOperationException();
  }
}