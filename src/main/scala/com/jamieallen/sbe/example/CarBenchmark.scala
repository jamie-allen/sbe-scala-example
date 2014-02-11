package com.jamieallen.sbe.example

import org.openjdk.jmh.annotations.{ GenerateMicroBenchmark, Scope, State }
import uk.co.real_logic.sbe.codec.java.DirectBuffer
import scala.util.{ Try, Failure, Success }
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import baseline._

object CarBenchmark {
  // Not an Option because we'll fail out if no value is obtained
  private var make, model, manufacturerCode, vehicleCode: Array[Byte] = null

  // Convert four Try's into a single Try of 
  // a tuple of four Array[Byte] for simplicity
  val baseValues = for {
    vc <- Try("abcdef".getBytes(Car.vehicleCodeCharacterEncoding))
    mc <- Try("123".getBytes(Engine.manufacturerCodeCharacterEncoding))
    ma <- Try("Honda".getBytes(Car.makeCharacterEncoding))
    mo <- Try("Civic VTi".getBytes(Car.modelCharacterEncoding))
  } yield (vc, mc, ma, mo)

  // Exit if any failure occurred, else encode/decode
  baseValues match {
    case Failure(e) =>
      println(e)
      System.exit(-1)
    case Success((vc: Array[Byte], mc: Array[Byte], ma: Array[Byte], mo: Array[Byte])) =>
      make = ma
      model = mo
      manufacturerCode = mc
      vehicleCode = vc
  }

  class MyState {
    val bufferIndex = 0
    val car = new Car()
    val messageHeader = new MessageHeader()
    val encodeBuffer = new DirectBuffer(ByteBuffer.allocateDirect(1024))

    val tempBuffer = Array.ofDim[Byte](128)
    val decodeBuffer = new DirectBuffer(ByteBuffer.allocateDirect(1024))

    {
      CarBenchmark.encode(messageHeader, car, decodeBuffer, bufferIndex);
    }
  }

  @State(Scope.Benchmark)
  val state = new MyState()

  @GenerateMicroBenchmark
  def testEncode(state: MyState) = {
    val car = state.car
    val messageHeader = state.messageHeader
    val buffer = state.encodeBuffer
    val bufferIndex = state.bufferIndex

    encode(messageHeader, car, buffer, bufferIndex)

    car.size
  }

  @GenerateMicroBenchmark
  def testDecode(state: MyState) = {
    val car = state.car
    val messageHeader = state.messageHeader
    val buffer = state.decodeBuffer
    val bufferIndex = state.bufferIndex
    val tempBuffer = state.tempBuffer

    decode(messageHeader, car, buffer, bufferIndex, tempBuffer)

    car.size
  }

  private def encode(messageHeader: MessageHeader,
    car: Car,
    buffer: DirectBuffer,
    bufferIndex: Int) = {
    messageHeader.wrap(buffer, bufferIndex, 0)
      .templateId(car.templateId)
      .version(car.templateVersion)
      .blockLength(car.blockLength)

    car.wrapForEncode(buffer, bufferIndex + messageHeader.size)
      .code(Model.A)
      .modelYear(2005)
      .serialNumber(12345)
      .available(BooleanType.TRUE)
      .putVehicleCode(vehicleCode, 0)

    for (i <- 0 to Car.someNumbersLength)
      car.someNumbers(i, i)

    car.extras.clear.sportsPack(true).sunRoof(true)

    car.engine.capacity(4200).numCylinders(8.asInstanceOf[Short]).putManufacturerCode(manufacturerCode, 0)

    car.fuelFiguresCount(3).next().speed(30).mpg(35.9f)
      .next().speed(55).mpg(49.0f)
      .next().speed(75).mpg(40.0f)

    val perfFigures = car.performanceFiguresCount(2)
    perfFigures.next.octaneRating(95.asInstanceOf[Short])
      .accelerationCount(3).next().mph(30).seconds(4.0f)
      .next.mph(60).seconds(7.5f)
      .next.mph(100).seconds(12.2f)
    perfFigures.next.octaneRating(99.asInstanceOf[Short])
      .accelerationCount(3).next().mph(30).seconds(3.8f)
      .next.mph(60).seconds(7.1f)
      .next.mph(100).seconds(11.8f)

    car.putMake(make, 0, make.length)
    car.putModel(model, 0, model.length)
  }

  private def decode(messageHeader: MessageHeader,
    car: Car,
    buffer: DirectBuffer,
    bufferIndex: Int,
    tempBuffer: Array[Byte]) = {
    messageHeader.wrap(buffer, bufferIndex, 0);

    val actingVersion = messageHeader.version
    val actingBlockLength = messageHeader.blockLength

    car.wrapForDecode(buffer, bufferIndex + messageHeader.size(), actingBlockLength, actingVersion)

    car.serialNumber
    car.modelYear
    car.available
    car.code

    for (i <- 0 to Car.someNumbersLength)
      car.someNumbers(i)

    for (i <- 0 to Car.vehicleCodeLength)
      car.vehicleCode(i)

    val extras = car.extras
    extras.cruiseControl
    extras.sportsPack
    extras.sunRoof

    val engine = car.engine
    engine.capacity
    engine.numCylinders
    engine.maxRpm
    for (i <- 0 to Engine.manufacturerCodeLength)
      engine.manufacturerCode(i)

    engine.getFuel(tempBuffer, 0, tempBuffer.length)

    {
      // Code block to limit scope of implicit conversion, even though I'm using
      // a facility that forces me to be explicit about it (I'm OCD about this)
      import scala.collection.convert.decorateAsScala.asScalaIteratorConverter
      car.fuelFigures.asScala.foreach { fuelFigures =>
        fuelFigures.speed
        fuelFigures.mpg
      }

      car.performanceFigures.asScala.foreach { performanceFigures =>
        performanceFigures.octaneRating
        performanceFigures.acceleration.asScala.foreach { acceleration =>
          acceleration.mph
          acceleration.seconds
        }
      }
    }

    car.getMake(tempBuffer, 0, tempBuffer.length)
    car.getModel(tempBuffer, 0, tempBuffer.length)
  }

  /*
   * Benchmarks to allow execution outside of JMH.
   */
  //  def main(args: Array[String]) = {
  //    for (i <- 0 to 10) {
  //      perfTestEncode(i)
  //      perfTestDecode(i)
  //    }
  //  }
  //
  //    private static void perfTestEncode(final int runNumber)
  //    {
  //        final int reps = 10 * 1000 * 1000;
  //        final MyState state = new MyState();
  //        final CarBenchmark benchmark = new CarBenchmark();
  //
  //        final long start = System.nanoTime();
  //        for (int i = 0; i < reps; i++)
  //        {
  //            benchmark.testEncode(state);
  //        }
  //
  //        final long totalDuration = System.nanoTime() - start;
  //
  //        System.out.printf("%d - %d(ns) average duration for %s.testEncode() - message size %d\n",
  //                          Integer.valueOf(runNumber),
  //                          Long.valueOf(totalDuration / reps),
  //                          benchmark.getClass().getName(),
  //                          Integer.valueOf(state.car.size() + state.messageHeader.size()));
  //    }
  //
  //    private static void perfTestDecode(final int runNumber)
  //    {
  //        final int reps = 10 * 1000 * 1000;
  //        final MyState state = new MyState();
  //        final CarBenchmark benchmark = new CarBenchmark();
  //
  //        final long start = System.nanoTime();
  //        for (int i = 0; i < reps; i++)
  //        {
  //            benchmark.testDecode(state);
  //        }
  //
  //        final long totalDuration = System.nanoTime() - start;
  //
  //        System.out.printf("%d - %d(ns) average duration for %s.testDecode() - message size %d\n",
  //                          Integer.valueOf(runNumber),
  //                          Long.valueOf(totalDuration / reps),
  //                          benchmark.getClass().getName(),
  //                          Integer.valueOf(state.car.size() + state.messageHeader.size()));
  //    }
}