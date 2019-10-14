/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.fastcatsearch.transport;

import java.io.EOFException;
import java.io.IOException;

import org.fastcatsearch.common.BytesReference;
import org.fastcatsearch.common.io.StreamInput;
import org.fastcatsearch.transport.common.ChannelBufferBytesReference;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * A Netty {@link org.jboss.netty.buffer.ChannelBuffer} based {@link org.elasticsearch.common.io.stream.StreamInput}.
 */
public class ChannelBufferStreamInput extends StreamInput {

    private final ChannelBuffer buffer;
    private final int startIndex;
    private final int endIndex;

    public ChannelBufferStreamInput(ChannelBuffer buffer) {
        this(buffer, buffer.readableBytes());
    }

    public ChannelBufferStreamInput(ChannelBuffer buffer, int length) {
    	
    	
        if (length > buffer.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        this.buffer = buffer;
        startIndex = buffer.readerIndex();
        endIndex = startIndex + length;
        buffer.markReaderIndex();
    }

    @Override
    public BytesReference readBytesReference(int length) throws IOException {
        ChannelBufferBytesReference ref = new ChannelBufferBytesReference(buffer.slice(buffer.readerIndex(), length));
        buffer.skipBytes(length);
        return ref;
    }

    @Override
    public int available() throws IOException {
        return endIndex - buffer.readerIndex();
    }

    @Override
    public void mark(int readlimit) {
        buffer.markReaderIndex();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        if (available() == 0) {
            return -1;
        }
        return buffer.readByte() & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int available = available();
        if (available == 0) {
            return -1;
        }

        len = Math.min(available, len);
        buffer.readBytes(b, off, len);
        return len;
    }

    @Override
    public void reset() throws IOException {
        buffer.resetReaderIndex();
    }

    @Override
    public long skip(long n) throws IOException {
        if (n > Integer.MAX_VALUE) {
            return skipBytes(Integer.MAX_VALUE);
        } else {
            return skipBytes((int) n);
        }
    }

    public int skipBytes(int n) throws IOException {
        int nBytes = Math.min(available(), n);
        buffer.skipBytes(nBytes);
        return nBytes;
    }


    @Override
    public byte readByte() throws IOException {
        return buffer.readByte();
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) throws IOException {
        int read = read(b, offset, len);
        if (read < len) {
            throw new EOFException();
        }
    }

    @Override
    public void close() throws IOException {
        // nothing to do here
    }

	@Override
	public void seek(long p) throws IOException {
		throw new IOException("seek는 지원하지 않습니다.");
	}

	@Override
	public long position() throws IOException {
		return buffer.readerIndex();
	}

	@Override
	public long length() throws IOException {
		return endIndex - startIndex;
	}
}
