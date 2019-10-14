package org.fastcatsearch.ir.search.clause;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

import org.fastcatsearch.ir.query.RankInfo;
import org.fastcatsearch.ir.query.RowExplanation;

/**
 * */
public class DefaultOperatedClause extends OperatedClause {
	
	private OperatedClause operatedClause;
	private String description;
	
	public DefaultOperatedClause(OperatedClause operatedClause, String description) {
		super("DEFAULT");
		this.operatedClause = operatedClause;
		this.description = description;
	}
	
	@Override
	protected boolean nextDoc(RankInfo docInfo) throws IOException {
		if(operatedClause == null){
			return false;
		}
		
		if(operatedClause.next(docInfo)){
			if(docInfo.rowExplanations() != null) {
				docInfo.rowExplanations().add(new RowExplanation(id, docInfo.score(), description));
			}
			return true;
		}
		
		return false;
	}

	@Override
	public void close() {
		if(operatedClause != null){
			operatedClause.close();
		}
	}

	@Override
	protected void initClause(boolean explain) throws IOException {
		operatedClause.init(explanation != null ? explanation.createSubExplanation() : null);
	}

	@Override
	public OperatedClause[] children() {
		return new OperatedClause[] { this.operatedClause };
	}

    @Override
    public void printTrace(Writer writer, int indent, int depth) throws IOException {

    }

    public DefaultOperatedClause clone(OperatedClause clause) {
		return new DefaultOperatedClause(clause, this.description);
	}
}
