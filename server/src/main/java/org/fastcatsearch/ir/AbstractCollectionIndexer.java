package org.fastcatsearch.ir;

import org.fastcatsearch.datasource.reader.DataSourceReader;
import org.fastcatsearch.ir.analysis.AnalyzerPoolManager;
import org.fastcatsearch.ir.common.IRException;
import org.fastcatsearch.ir.common.SettingException;
import org.fastcatsearch.ir.config.CollectionContext;
import org.fastcatsearch.ir.config.CollectionIndexStatus.IndexStatus;
import org.fastcatsearch.ir.config.DataInfo.SegmentInfo;
import org.fastcatsearch.ir.config.IndexConfig;
import org.fastcatsearch.ir.document.Document;
import org.fastcatsearch.ir.index.*;
import org.fastcatsearch.ir.settings.Schema;
import org.fastcatsearch.ir.settings.SchemaSetting;
import org.fastcatsearch.ir.util.Formatter;
import org.fastcatsearch.job.indexing.IndexingStopException;
import org.fastcatsearch.job.state.IndexingTaskState;
import org.fastcatsearch.util.CollectionContextUtil;
import org.fastcatsearch.util.FilePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public abstract class AbstractCollectionIndexer implements CollectionIndexerable {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractCollectionIndexer.class);
    protected static Logger indexingLogger = LoggerFactory.getLogger("INDEXING_LOG");
	protected CollectionContext collectionContext;
	protected AnalyzerPoolManager analyzerPoolManager;
	
	protected DataSourceReader dataSourceReader;
	protected long startTime;
	protected IndexingTaskState indexingTaskState;
	
	protected DeleteIdSet deleteIdSet; //삭제문서리스트. 외부에서 source reader를 통해 셋팅된다.
	
	protected IndexWriteInfoList indexWriteInfoList;
	
	protected IndexWritable indexWriter;
	protected SegmentInfo workingSegmentInfo;
	protected int count;
	protected long lapTime;
	
	protected boolean stopRequested;
	protected SelectedIndexList selectedIndexList;// 색인필드 선택사항.
	
	public AbstractCollectionIndexer(CollectionContext collectionContext, AnalyzerPoolManager analyzerPoolManager) {
		this(collectionContext, analyzerPoolManager, null);
	}
	public AbstractCollectionIndexer(CollectionContext collectionContext, AnalyzerPoolManager analyzerPoolManager, SelectedIndexList selectedIndexList) {
		this.collectionContext = collectionContext;
		this.analyzerPoolManager = analyzerPoolManager;
		this.selectedIndexList = selectedIndexList;
	}
	
	protected abstract DataSourceReader createDataSourceReader(File filePath, SchemaSetting schemaSetting) throws IRException, IOException;
	protected abstract void prepare() throws IRException;
	protected abstract boolean done(SegmentInfo segmentInfo, IndexStatus indexStatus) throws IRException, IndexingStopException;
	protected IndexWritable createIndexWriter(Schema schema, File segmentDir, SegmentInfo segmentInfo, IndexConfig indexConfig) throws IRException {
		return new SegmentWriter(schema, segmentDir, segmentInfo, indexConfig, analyzerPoolManager, selectedIndexList);
	}

    public SegmentInfo getSegmentInfo() {
        return workingSegmentInfo;
    }

	public void init(Schema schema) throws IRException, IOException {

		prepare();
		
		FilePaths dataFilePaths = collectionContext.collectionFilePaths().dataPaths();
		int dataSequence = collectionContext.getIndexSequence();

		IndexConfig indexConfig = collectionContext.indexConfig();
		
		logger.debug("WorkingSegmentInfo = {}", workingSegmentInfo);
		String segmentId = workingSegmentInfo.getId();

		File segmentDir = dataFilePaths.segmentFile(dataSequence, segmentId);
		logger.info("Segment Dir = {}", segmentDir.getAbsolutePath());
		
		
		File filePath = collectionContext.collectionFilePaths().file();
		dataSourceReader = createDataSourceReader(filePath, schema.schemaSetting());
		
		indexWriter = createIndexWriter(schema, segmentDir, workingSegmentInfo, indexConfig);
		
		indexWriteInfoList = new IndexWriteInfoList();
		
		startTime = System.currentTimeMillis();
	}

	public void addDocument(Document document) throws IRException, IOException{
		indexWriter.addDocument(document);
		count++;
		if (count % 10000 == 0) {
            indexingLogger.info(
					"[{}] Full Indexing {} ... lap = {} ms, elapsed = {}, mem = {}",
					collectionContext.collectionId(), count, System.currentTimeMillis() - lapTime,
							Formatter.getFormatTime(System.currentTimeMillis() - startTime),
							Formatter.getFormatSize(Runtime.getRuntime().totalMemory()));
			lapTime = System.currentTimeMillis();
		}
		if(indexingTaskState != null){
			indexingTaskState.incrementDocumentCount();
		}
	}
	@Override
	public void requestStop(){
        indexingLogger.info("[{}] Indexer Stop Requested! ", collectionContext.collectionId());
		
		stopRequested = true;
	}
	
	//색인취소(0건)이면 false;
	@Override
	public boolean close() throws IRException, SettingException, IndexingStopException {
		
//		RevisionInfo revisionInfo = workingSegmentInfo.getRevisionInfo();
		if (indexWriter != null) {
			try {
				indexWriter.close();
				if(indexWriter instanceof WriteInfoLoggable)
				((WriteInfoLoggable) indexWriter).getIndexWriteInfo(indexWriteInfoList);
			} catch (IOException e) {
				throw new IRException(e);
			}
		}

		dataSourceReader.close();
		
		logger.debug("##Indexer close {}", workingSegmentInfo);
		deleteIdSet = dataSourceReader.getDeleteList();
		int deleteCount = 0;
		if(deleteIdSet != null) {
			deleteCount = deleteIdSet.size();
		}

        workingSegmentInfo.setDeleteCount(deleteCount);
		
		long endTime = System.currentTimeMillis();
		
		IndexStatus indexStatus = new IndexStatus(workingSegmentInfo.getDocumentCount(), deleteCount,
				Formatter.formatDate(new Date(startTime)), Formatter.formatDate(new Date(endTime)), Formatter.getFormatTime(endTime - startTime));
		
		if(done(workingSegmentInfo, indexStatus)){
			CollectionContextUtil.saveCollectionAfterIndexing(collectionContext);
		}else{
			//저장하지 않음.
		}
		return true;
	}
	
	public IndexWriteInfoList indexWriteInfoList() {
		return indexWriteInfoList;
	}
	@Override
	public void doIndexing() throws IRException, IOException {
		
		indexingTaskState.setStep(IndexingTaskState.STEP_INDEXING);
		
		lapTime = System.currentTimeMillis();
		while (dataSourceReader.hasNext()) {
			if(stopRequested){
				break;
			}
			Document document = dataSourceReader.nextDocument();
//			logger.debug("doc >> {}", document);
			addDocument(document);
		}

	}
	
	public DeleteIdSet deleteIdSet() {
		return deleteIdSet;
	}
	
	public void setTaskState(IndexingTaskState indexingTaskState) {
		this.indexingTaskState = indexingTaskState;
	}
}
