package org.fastcatsearch.ir.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharsRefTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public class NGramWordTokenizer extends Tokenizer {
	private int minGram;
	private int maxGram;

	private char[] charBuffer;
	private char[] readBuffer;
	private int length;

	private int nGram;
	private int pos;
	private final CharsRefTermAttribute termAttribute = addAttribute(CharsRefTermAttribute.class);
    private final PositionIncrementAttribute positionAttribute = addAttribute(PositionIncrementAttribute.class);

	public NGramWordTokenizer(Reader input) {
		this(input, 1, 3);
	}

	public NGramWordTokenizer(Reader input, int minGram, int maxGram) {
		super(input);
		this.minGram = minGram;
		this.maxGram = maxGram;
		readBuffer = new char[1024];
		nGram = minGram;

		
	}
	
	@Override
	public void reset() throws IOException {
		if (analyzerOption != null) {
			if (analyzerOption.isForDocument()) {
				fillWithoutSpace();
			} else if (analyzerOption.isForQuery()) {
				fillWithSpace();
			}
		} else {
			fillWithoutSpace();
		}
	}
	
	
	private void fillWithSpace() throws IOException {
		charBuffer = null;
		int n = 0;

		while ((n = input.read(readBuffer)) != -1) {
			if (n > 0) {
				if (charBuffer == null) {
					charBuffer = new char[n];
					System.arraycopy(readBuffer, 0, charBuffer, 0, n);
				} else {
					char[] newCharBuffer = new char[charBuffer.length + n];
					System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
					System.arraycopy(readBuffer, 0, newCharBuffer, charBuffer.length, n);
					charBuffer = newCharBuffer;
				}
			}
		}
		
		
		if(charBuffer!=null) {
			length = charBuffer.length; 
		} else {
			length = 0;
		}
		pos = 0;
		nGram = minGram;
	}
	
	private void fillWithoutSpace() throws IOException {
		fillWithSpace();
		
		length = 0;
		if(charBuffer != null) {
			for (int i = 0; i < charBuffer.length; i++) {
				if (charBuffer[i] <= 32) {
					continue;
				}
	
				charBuffer[length++] = (char) charBuffer[i];
			}
		}
	}
	
//	@Override
//	public void setReader(Reader input) throws IOException {
//		super.setReader(input);
//		reset();
//	}
	
	@Override
	public boolean incrementToken() throws IOException {
		while (pos < length) {

			LABEL_POS:
			while (nGram <= maxGram) {

				if (pos + nGram <= length) {
//					System.out.println(new String(charBuffer, pos, nGram) + ">>>>>>" + charBuffer[pos] + " : " + charBuffer[pos + nGram - 1]);
					for(int i = pos; i < pos + nGram ; i++) {
						if(charBuffer[i] == ' '){
							if(nGram < maxGram){
								nGram++;
//								System.out.println("$$$$ continue;");
								continue LABEL_POS;
							}else{
								pos++;
								nGram = minGram;
//								System.out.println("$$$$ break;");
								break LABEL_POS;
							}
						}
					}
					termAttribute.setBuffer(charBuffer, pos, nGram);
                    positionAttribute.setPositionIncrement(pos);
					if (nGram == maxGram) {
						nGram = minGram;
						pos++;
					} else {
						nGram++;
					}
					return true;
				}

				if (nGram == maxGram) {
					nGram = minGram;
					pos++;
					break;
					// 다음 pos로 옮겨서 minGram부터 분석시작.
				} else {
					nGram++;
				}

			}
		}

		return false;
	}

}
