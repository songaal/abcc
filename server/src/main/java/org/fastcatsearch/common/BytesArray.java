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

package org.fastcatsearch.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.fastcatsearch.common.io.BytesStreamInput;
import org.fastcatsearch.common.io.StreamInput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class BytesArray implements BytesReference {

    public static final BytesArray EMPTY = new BytesArray(Bytes.EMPTY_ARRAY, 0, 0);

    private byte[] bytes;
    private int offset;
    private int length;

    public BytesArray(byte[] bytes) {
        this.bytes = bytes;
        this.offset = 0;
        this.length = bytes.length;
    }

    public BytesArray(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public byte get(int index) {
        return bytes[offset + index];
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public BytesReference slice(int from, int length) {
        if (from < 0 || (from + length) > this.length) {
            throw new IllegalArgumentException("can't slice a buffer with length [" + this.length + "], with slice parameters from [" + from + "], length [" + length + "]");
        }
        return new BytesArray(bytes, offset + from, length);
    }

    @Override
    public StreamInput streamInput() {
        return new BytesStreamInput(bytes, offset, length, false);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        os.write(bytes, offset, length);
    }

    @Override
    public byte[] toBytes() {
        if (offset == 0 && bytes.length == length) {
            return bytes;
        }
        return Arrays.copyOfRange(bytes, offset, offset + length);
    }

    @Override
    public BytesArray toBytesArray() {
        return this;
    }

    @Override
    public BytesArray copyBytesArray() {
        return new BytesArray(Arrays.copyOfRange(bytes, offset, offset + length));
    }

    @Override
    public ChannelBuffer toChannelBuffer() {
        return ChannelBuffers.wrappedBuffer(bytes, offset, length);
    }

    @Override
    public boolean hasArray() {
        return true;
    }

    @Override
    public byte[] array() {
        return bytes;
    }

    @Override
    public int arrayOffset() {
        return offset;
    }

    @Override
    public int hashCode() {
        return Helper.bytesHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return Helper.bytesEqual(this, (BytesReference) obj);
    }

//	@Override
//	public String toUtf8() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}