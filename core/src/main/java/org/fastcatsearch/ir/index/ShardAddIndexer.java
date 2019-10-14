//package org.fastcatsearch.ir.index;
//
//import java.io.File;
//import java.io.IOException;
//
//import org.apache.commons.io.FileUtils;
//import org.fastcatsearch.ir.common.IRException;
//import org.fastcatsearch.ir.common.IndexFileNames;
//import org.fastcatsearch.ir.common.IndexingType;
//import org.fastcatsearch.ir.config.DataInfo.RevisionInfo;
//import org.fastcatsearch.ir.config.DataInfo.SegmentInfo;
//import org.fastcatsearch.ir.config.DataPlanConfig;
//import org.fastcatsearch.ir.search.SegmentReader;
//import org.fastcatsearch.ir.search.ShardHandler;
//import org.fastcatsearch.ir.settings.Schema;
//import org.fastcatsearch.util.CoreFileUtils;
//
//public class ShardAddIndexer extends ShardIndexer {
//	private ShardHandler shardHandler;
//
//	public ShardAddIndexer(Schema schema, ShardHandler shardHandler) throws IRException {
//		super(shardHandler.shardContext());
//		this.shardHandler = shardHandler;
//		init(schema);
//	}
//
//	@Override
//	protected void prepare() throws IRException {
//		DataPlanConfig dataPlanConfig = shardContext.dataPlanConfig();
//		// 증분색인이면 기존스키마그대로 사용.
//
//		SegmentReader lastSegmentReader = shardHandler.getLastSegmentReader();
//		SegmentInfo segmentInfo = null;
//
//		try {
//			if (lastSegmentReader != null) {
//				segmentInfo = lastSegmentReader.segmentInfo();
//				int docCount = segmentInfo.getRevisionInfo().getDocumentCount();
//				int segmentDocumentLimit = dataPlanConfig.getSegmentDocumentLimit();
//
//				if (docCount >= segmentDocumentLimit) {
//					// segment가 생성되는 증분색인.
//					workingSegmentInfo = segmentInfo.getNextSegmentInfo();
//					File segmentDir = shardContext.collectionFilePaths().file(workingSegmentInfo.getId());
//					logger.debug("#색인시 세그먼트를 생성합니다. {}", workingSegmentInfo);
//					CoreFileUtils.removeDirectoryCascade(segmentDir);
//				} else {
//					// 기존 segment에 append되는 증분색인.
//					workingSegmentInfo = segmentInfo.copy();
//					// 리비전을 증가시킨다.
//					logger.debug("#old seginfo {}", workingSegmentInfo);
//					int revision = workingSegmentInfo.nextRevision();
//					File segmentDir = shardContext.collectionFilePaths().file(workingSegmentInfo.getId());
//					File revisionDir = new File(segmentDir, Integer.toString(revision));
//					CoreFileUtils.removeDirectoryCascade(revisionDir);
//					logger.debug("#색인시 리비전을 증가합니다. {}", workingSegmentInfo);
//				}
//			} else {
//				// TODO 전체색인이 없는데 증분색인이 가능하도록 해야하나?
//
//				// 로딩된 세그먼트가 없음.
//				// 이전 색인정보가 없다. 즉 전체색인이 수행되지 않은 컬렉션.
//				// segment가 생성되는 증분색인.
//				workingSegmentInfo = new SegmentInfo();
//				File segmentDir = shardContext.collectionFilePaths().file(workingSegmentInfo.getId());
//				logger.debug("#이전 세그먼트가 없어서 색인시 세그먼트를 생성합니다. {}", workingSegmentInfo);
//				CoreFileUtils.removeDirectoryCascade(segmentDir);
//			}
//		} catch (IOException e) {
//			throw new IRException(e);
//		}
//		workingSegmentInfo.resetRevisionInfo();
//
//	}
//
//	@Override
//	protected void done() throws IRException {
//
//		RevisionInfo revisionInfo = workingSegmentInfo.getRevisionInfo();
//
//		int insertCount = revisionInfo.getInsertCount();
//		int deleteCount = revisionInfo.getDeleteCount();
//
//		try {
//			if (insertCount > 0 || deleteCount > 0) {
//				if (insertCount > 0) {
//					revisionInfo.setRefWithRevision();
//				} else {
//					// 추가문서가 없고 삭제문서만 존재할 경우
//					logger.debug("추가문서없이 삭제문서만 존재합니다.!!");
//					if (workingSegmentInfo != null && !workingSegmentInfo.equals(workingSegmentInfo)) {
//						// 기존색인문서수가 limit을 넘으면서 삭제문서만 색인될 경우 세그먼트가 바뀌는 현상이 나타날수 있다.
//						// 색인후 문서가 0건이고 delete문서가 존재하면 이전 세그먼트의 다음 리비전으로 변경해주는 작업필요.
//						// 세그먼트가 다르면, 즉 증가했으면 다시 원래의 세그먼트로 돌리고, rev를 증가시킨다.
//						File segmentDir = shardContext.collectionFilePaths().file(workingSegmentInfo.getId());
//						FileUtils.deleteDirectory(segmentDir);
//
//						logger.debug("# 추가문서가 없으므로, segment를 삭제합니다. {}", segmentDir.getAbsolutePath());
//						workingSegmentInfo = workingSegmentInfo.copy();
//						int revision = workingSegmentInfo.getRevision();
//						workingSegmentInfo.getRevisionInfo().setInsertCount(0);
//						workingSegmentInfo.getRevisionInfo().setUpdateCount(0);
//						workingSegmentInfo.getRevisionInfo().setDeleteCount(deleteCount);
//
//						// 이전 리비전의 delete.set.#을 현 리비전으로 복사해온다.
//						// 원래 primarykeyindexeswriter에서 append일 경우 복사를 하나, 여기서는 추가문서가 0이므로
//						String segmentId = workingSegmentInfo.getId();
//						segmentDir = shardContext.collectionFilePaths().file(workingSegmentInfo.getId());
//						File revisionDir = IndexFileNames.getRevisionDir(segmentDir, revision);
//						File prevRevisionDir = IndexFileNames.getRevisionDir(segmentDir, revision - 1);
//						String deleteFileName = IndexFileNames.getSuffixFileName(IndexFileNames.docDeleteSet, segmentId);
//						FileUtils.copyFile(new File(prevRevisionDir, deleteFileName), new File(revisionDir, deleteFileName));
//					}
//					/*
//					 * else 세그먼트가 증가하지 않고 리비전이 증가한 경우.
//					 */
//				}
//				
//				
//				File segmentDir = shardContext.collectionFilePaths().file(workingSegmentInfo.getId());
//				shardHandler.updateShard(shardContext, workingSegmentInfo, segmentDir, deleteIdSet);
//				
//				//status.xml 업데이트
//				shardContext.updateIndexingStatus(IndexingType.ADD, revisionInfo, startTime, System.currentTimeMillis());
//			} else {
//				// 추가,삭제 문서 모두 없을때.
//				logger.info("[{}] Indexing Canceled due to no documents.", shardContext.shardId());
//
//				// 리비전 디렉토리 삭제.
//				File segmentDir = shardContext.collectionFilePaths().file(workingSegmentInfo.getId());
//				File revisionDir = IndexFileNames.getRevisionDir(segmentDir, revisionInfo.getId());
//				if (workingSegmentInfo != null && !workingSegmentInfo.equals(workingSegmentInfo)) {
//					// 세그먼트 증가시 segment디렉토리 삭제.
//					FileUtils.deleteDirectory(segmentDir);
//					logger.info("delete segment dir ={}", segmentDir.getAbsolutePath());
//				} else {
//					// 리비전 증가시 revision디렉토리 삭제.
//					FileUtils.deleteDirectory(revisionDir);
//					logger.info("delete revision dir ={}", revisionDir.getAbsolutePath());
//				}
//			}
//			
//		} catch (IOException e) {
//			throw new IRException(e);
//		}
//		
//
//	}
//
//}
