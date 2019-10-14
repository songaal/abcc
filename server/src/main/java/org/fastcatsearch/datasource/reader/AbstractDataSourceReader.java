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

package org.fastcatsearch.datasource.reader;

import org.fastcatsearch.ir.common.IRException;
import org.fastcatsearch.ir.document.Document;
import org.fastcatsearch.ir.index.DeleteIdSet;
import org.fastcatsearch.ir.settings.FieldSetting;
import org.fastcatsearch.ir.settings.PrimaryKeySetting;
import org.fastcatsearch.ir.settings.SchemaSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 데이터소스 리더.
 * 
 * DataSourceReader는 SingleSourceReader를 받아서 처리한다.
 * SingleSourceReader는 field별 데이터를 셋팅한뒤, modify 까지 수행후 document를 만들어서 최종리턴을 한다.
 * 내부 SingleSourceReader 가 여러개이면 멀티소스리더같이 동작하게 된다.
 * */
public abstract class AbstractDataSourceReader<DataType> implements DataSourceReader {

	protected static Logger logger = LoggerFactory.getLogger(AbstractDataSourceReader.class);

	protected List<FieldSetting> fieldSettingList;

	private DeleteIdSet deleteIdList;

	private List<SingleSourceReader<DataType>> singleSourceReaderList;
	private int readerPos;
	private SingleSourceReader<DataType> currentReader;

	// DataSourceConfig안에는 SingleSourceConfig가 여러개 들어있다.
	public AbstractDataSourceReader(SchemaSetting schemaSetting) throws IRException {
		fieldSettingList = schemaSetting.getFieldSettingList();

		PrimaryKeySetting primaryKeySetting = schemaSetting.getPrimaryKeySetting();
		if (primaryKeySetting != null && primaryKeySetting.getFieldList() != null && primaryKeySetting.getFieldList().size() > 0) {
			int pkFieldSize = primaryKeySetting.getFieldList().size();
			deleteIdList = new DeleteIdSet(pkFieldSize);
		}

		singleSourceReaderList = new ArrayList<SingleSourceReader<DataType>>();
	}

	private void nextReader() throws IRException, IOException {
		while (readerPos < singleSourceReaderList.size()) {
			currentReader = singleSourceReaderList.get(readerPos++);
			if (!currentReader.isActive()) {
				continue;
			}
			if (currentReader != null) {
                currentReader.init();
				return;
			}else{
				break;
			}
		}
		
		currentReader = null;
	}

	public final DeleteIdSet getDeleteList() {
		return deleteIdList;
	}

	public void addSourceReader(SingleSourceReader<DataType> sourceReader) throws IRException {
		sourceReader.setDeleteIdList(deleteIdList);
		singleSourceReaderList.add(sourceReader);
	}

	public void init() throws IRException, IOException {
		nextReader();
	}
	public boolean hasNext() throws IRException {
		try {
			while (true) {
				if (currentReader == null) {
					// 다음번 reader가 null이면 모두 읽은 것임.
					return false;
				}

				if (currentReader.hasNext()) {
					return true;
				} else {
					currentReader.close();
					// 이번리더를 다 읽었으면 다음 리더로 이동.
					nextReader();
				}
			}
		} catch (Throwable e) {
			close();
			throw new IRException(e);
		}
	}

	public Document nextDocument() throws IRException, IOException {
		return createDocument(currentReader.nextElement());
	}

	protected abstract Document createDocument(DataType nextElement) throws IRException;

	public void close() {
		for (SingleSourceReader<DataType> reader : singleSourceReaderList) {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IRException e) {
			}
		}
	}

}
