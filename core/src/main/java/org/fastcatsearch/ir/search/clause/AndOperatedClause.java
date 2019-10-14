/*
 * Copyright 2013 Websquared, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fastcatsearch.ir.search.clause;

import org.fastcatsearch.ir.query.RankInfo;

import java.io.IOException;
import java.io.Writer;

public class AndOperatedClause extends OperatedClause {
    private OperatedClause clause1;
    private OperatedClause clause2;
    private boolean hasNext1 = true;
    private boolean hasNext2 = true;
    private RankInfo docInfo1;
    private RankInfo docInfo2;
    private int proximity;

    private boolean needsPositions;

    public AndOperatedClause(OperatedClause clause1, OperatedClause clause2) {
        super("AND");
        this.clause1 = clause1;
        this.clause2 = clause2;
    }

    public AndOperatedClause(OperatedClause clause1, OperatedClause clause2, int proximity) {
        this(clause1, clause2);
        this.proximity = proximity;
    }
    public AndOperatedClause(OperatedClause clause1, OperatedClause clause2, int proximity, boolean needsPositions) {
        this(clause1, clause2);
        this.proximity = proximity;
        this.needsPositions = needsPositions;
    }

    protected boolean nextDoc(RankInfo rankInfo) throws IOException {
        while(true) {
            if(needsPositions) {
                docInfo1.clearOccurrence();
                docInfo2.clearOccurrence();
            }
            hasNext1 = clause1.next(docInfo1);
            hasNext2 = clause2.next(docInfo2);

            if (hasNext1 && hasNext2) {
                int doc1 = docInfo1.docNo();
                int doc2 = docInfo2.docNo();
                while (hasNext1 && hasNext2 && (doc1 != doc2)) {
                    while (hasNext1 && (doc1 < doc2)) {
                        if(needsPositions) {
                            docInfo1.clearOccurrence();
                        }
                        hasNext1 = clause1.next(docInfo1);
                        doc1 = docInfo1.docNo();
                    }
                    while (hasNext2 && (doc1 > doc2)) {
                        if(needsPositions) {
                            docInfo2.clearOccurrence();
                        }
                        hasNext2 = clause2.next(docInfo2);
                        doc2 = docInfo2.docNo();
                    }
                }

                if (hasNext1 && hasNext2 && (doc1 == doc2)) {

                    rankInfo.explain(docInfo1);
                    rankInfo.explain(docInfo2);
                    if (needsPositions) {
                        rankInfo.addTermOccurrencesList(docInfo1.getTermOccurrencesList());
                        rankInfo.addTermOccurrencesList(docInfo2.getTermOccurrencesList());
                    }
                    //positions는 doc2(나중 텀)의 것 을 넣어준다.
                    rankInfo.init(doc1, docInfo1.score() + docInfo2.score(), docInfo1.hit() + docInfo2.hit());
                    return true;
                }

                return false;
            } else {
                //절1과 절2중 하나라도 끝나면 AND 집합도 더이상 없는것이다.
                return false;
            }
        }
    }



    @Override
    public String toString(){
        return "["+getClass().getSimpleName()+"]"
                + (clause1!= null?clause1.toString():"null") + " / "
                + (clause2!= null?clause2.toString():"null");
    }

    @Override
    public void close() {
        if(clause1 != null){
            clause1.close();
        }
        if(clause2 != null){
            clause2.close();
        }
    }

    @Override
    protected void initClause(boolean explain) throws IOException {
        docInfo1 = new RankInfo(explain);
        docInfo2 = new RankInfo(explain);
        if(clause1 != null) {
            clause1.init(explanation != null ? explanation.createSubExplanation() : null);
        }

        if(clause2 != null) {
            clause2.init(explanation != null ? explanation.createSubExplanation() : null);
        }
    }

    @Override
    public void printTrace(Writer writer, int indent, int depth) throws IOException {
        String indentSpace = "";
        if(depth > 0){
            for (int i = 0; i < (depth - 1) * indent; i++) {
                indentSpace += " ";
            }

            for (int i = (depth - 1) * indent, p = 0; i < depth * indent; i++, p++) {
                if(p == 0){
                    indentSpace += "|";
                }else{
                    indentSpace += "-";
                }
            }
        }
        writer.append(indentSpace).append("[AND]\n");
        if(clause1 != null){
            clause1.printTrace(writer, indent, depth + 1);
        }
        if(clause2 != null){
            clause2.printTrace(writer, indent, depth + 1);
        }
    }

    @Override
    public OperatedClause[] children() {
        return new OperatedClause[] {
                clause1,
                clause2
        };
    }

//	@Override
//	protected void initExplanation() {
//		if(clause1 != null) {
//			clause1.setExplanation(explanation.createSub1());
//		}
//		if(clause2 != null) {
//			clause2.setExplanation(explanation.createSub2());
//		}
//	}

}
