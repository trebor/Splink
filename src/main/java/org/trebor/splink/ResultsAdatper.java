package org.trebor.splink;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

public class ResultsAdatper implements ResultsListener
{
  public int onTuple(TupleQueryResult result)
    throws QueryEvaluationException
  {
    throw new UnsupportedOperationException();
  }

  public int onGraph(GraphQueryResult result)
    throws QueryEvaluationException
  {
    throw new UnsupportedOperationException();
  }

  public boolean onBoolean(boolean result)
  {
    throw new UnsupportedOperationException();
  }

  public void onUpdate()
  {
    throw new UnsupportedOperationException();
  }
}