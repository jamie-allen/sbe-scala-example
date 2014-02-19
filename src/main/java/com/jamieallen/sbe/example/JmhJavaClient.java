package com.jamieallen.sbe.example;

import java.nio.ByteBuffer;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import uk.co.real_logic.sbe.codec.java.DirectBuffer;
import baseline.Car;
import baseline.MessageHeader;

public class JmhJavaClient {
	@State(Scope.Benchmark)
	public static class MyState {
		final int bufferIndex = 0;
		final Car car = new Car();
		final MessageHeader messageHeader = new MessageHeader();
		final DirectBuffer encodeBuffer = new DirectBuffer(
		    ByteBuffer.allocateDirect(1024));

		final byte[] tempBuffer = new byte[128];
		final DirectBuffer decodeBuffer = new DirectBuffer(
		    ByteBuffer.allocateDirect(1024));

		{
			CarBenchmark.encode(messageHeader, car, decodeBuffer, bufferIndex);
		}
	}

	@GenerateMicroBenchmark
	public int testEncode(final MyState state) {
		final Car car = state.car;
		final MessageHeader messageHeader = state.messageHeader;
		final DirectBuffer buffer = state.encodeBuffer;
		final int bufferIndex = state.bufferIndex;

		CarBenchmark.encode(messageHeader, car, buffer, bufferIndex);

		return car.size();
	}

	@GenerateMicroBenchmark
	public int testDecode(final MyState state) {
		final Car car = state.car;
		final MessageHeader messageHeader = state.messageHeader;
		final DirectBuffer buffer = state.decodeBuffer;
		final int bufferIndex = state.bufferIndex;
		final byte[] tempBuffer = state.tempBuffer;

		CarBenchmark.decode(messageHeader, car, buffer, bufferIndex, tempBuffer);

		return car.size();
	}

}
