package com.jamieallen.sbe.example

import org.openjdk.jmh.annotations.{ Benchmark, Scope, State }
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
    case _ =>
      println("No error found, but four byte arrays were not returned from the String to Array[Byte] conversion")
      System.exit(-1)
  }

  @State(Scope.Benchmark)
  class MyState {
    val bufferIndex = 0
    val car = new Car()
    val messageHeader = new MessageHeader()
    val encodeBuffer = new DirectBuffer(ByteBuffer.allocateDirect(1024))

    val tempBuffer = Array.ofDim[Byte](128)
    val decodeBuffer = new DirectBuffer(ByteBuffer.allocateDirect(1024))

    {
      encode(messageHeader, car, decodeBuffer, bufferIndex);
    }
  }
  
  def encode(messageHeader: MessageHeader, car: Car, buffer: DirectBuffer, bufferIndex: Int) = {
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

    for (i <- 0 to (Car.someNumbersLength - 1))
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

  def decode(messageHeader: MessageHeader, car: Car, buffer: DirectBuffer, bufferIndex: Int, tempBuffer: Array[Byte]) = {
    messageHeader.wrap(buffer, bufferIndex, 0);

    val actingVersion = messageHeader.version
    val actingBlockLength = messageHeader.blockLength

    car.wrapForDecode(buffer, bufferIndex + messageHeader.size(), actingBlockLength, actingVersion)

    car.serialNumber
    car.modelYear
    car.available
    car.code

    for (i <- 0 to (Car.someNumbersLength - 1))
      car.someNumbers(i)

    for (i <- 0 to (Car.vehicleCodeLength - 1))
      car.vehicleCode(i)

    val extras = car.extras
    extras.cruiseControl
    extras.sportsPack
    extras.sunRoof

    val engine = car.engine
    engine.capacity
    engine.numCylinders
    engine.maxRpm
    for (i <- 0 to (Engine.manufacturerCodeLength - 1))
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
}

class CarBenchmark {
  @Benchmark
  def testEncode(state: CarBenchmark.MyState) = {
    val car = state.car
    val messageHeader = state.messageHeader
    val buffer = state.encodeBuffer
    val bufferIndex = state.bufferIndex
  
    CarBenchmark.encode(messageHeader, car, buffer, bufferIndex)
    car.size
  }
  
  @Benchmark
  def testDecode(state: CarBenchmark.MyState) = {
    val car = state.car
    val messageHeader = state.messageHeader
    val buffer = state.decodeBuffer
    val bufferIndex = state.bufferIndex
    val tempBuffer = state.tempBuffer
  
    CarBenchmark.decode(messageHeader, car, buffer, bufferIndex, tempBuffer)
	  car.size
  }  
}