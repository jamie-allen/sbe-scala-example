package com.jamieallen.sbe.example

import baseline._
import java.io.{ FileOutputStream, UnsupportedEncodingException }
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import scala.util.{ Try, Failure, Success }
import uk.co.real_logic.sbe.codec.java.DirectBuffer

object SbeExample extends App {
  private val encodingFilename: String = "sbe.encoding.filename"
  private val messageHeader = new MessageHeader()
  private val car = new Car()

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
    case Success((vehicleCode: Array[Byte], manufacturerCode: Array[Byte], make: Array[Byte], model: Array[Byte])) =>
      encodeDecode(vehicleCode, manufacturerCode, make, model)
    case _ =>
      println("No error found, but four byte arrays were not returned from the String to Array[Byte] conversion")
      System.exit(-1)
  }

  private def encodeDecode(vehicleCode: Array[Byte], manufacturerCode: Array[Byte], make: Array[Byte], model: Array[Byte]) {
    val messageHeader = new MessageHeader()
    val car = new Car()

    val (directBuffer, messageTemplateVersion) = writeEncodedBytes(vehicleCode, manufacturerCode, make, model, messageHeader, car)
    readDecodedBytes(messageHeader, car, directBuffer, messageTemplateVersion)
  }

  private def readDecodedBytes(messageHeader: baseline.MessageHeader, 
                               car: baseline.Car, 
                               directBuffer: DirectBuffer, 
                               messageTemplateVersion: Short): 
                               Unit = {
    messageHeader.wrap(directBuffer, 0, messageTemplateVersion)

    // Lookup the applicable fly weight to decode this type of message based on templateId and version.
    val templateId = messageHeader.templateId
    val actingVersion: Short = messageHeader.version
    val actingBlockLength = messageHeader.blockLength

    val decodeBufferOffset = messageHeader.size
    decode(car, directBuffer, decodeBufferOffset, actingBlockLength, actingVersion)
  }

  private def writeEncodedBytes(vehicleCode: Array[Byte], 
                                manufacturerCode: Array[Byte], 
                                make: Array[Byte], 
                                model: Array[Byte], 
                                messageHeader: baseline.MessageHeader, 
                                car: baseline.Car): 
                                (DirectBuffer, Short) = {
    val byteBuffer = ByteBuffer.allocateDirect(4096)
    val directBuffer = new DirectBuffer(byteBuffer)
    val messageTemplateVersion: Short = 0

    // Setup for encoding a message
    messageHeader.wrap(directBuffer, 0, messageTemplateVersion)
      .blockLength(car.blockLength)
      .templateId(car.templateId)
      .version(car.templateVersion)

    val encodingLength = messageHeader.size + encode(car,
      directBuffer,
      messageHeader.size,
      vehicleCode,
      manufacturerCode,
      make,
      model)

    // Optionally write the encoded buffer to a file for decoding by the On-The-Fly decoder
    System.getProperty(encodingFilename) match {
      case encodingFName if encodingFName != null =>
        val channel = new FileOutputStream(encodingFName).getChannel
        byteBuffer.limit(encodingLength)
        channel.write(byteBuffer)
      case _ =>
    }
    (directBuffer, messageTemplateVersion)
  }

  private def encode(car: Car, 
                     directBuffer: DirectBuffer, 
                     bufferOffset: Int, 
                     vehicleCode: Array[Byte], 
                     manufacturerCode: Array[Byte], 
                     make: Array[Byte], 
                     model: Array[Byte]): 
                     Int = {
    val srcOffset = 0

    car.wrapForEncode(directBuffer, bufferOffset).
      serialNumber(1234).
      modelYear(2013).
      available(BooleanType.TRUE).
      code(Model.A).
      putVehicleCode(vehicleCode, srcOffset)

    for (n <- 0 to (Car.someNumbersLength - 1))
      car.someNumbers(n, n)

    car.extras.clear
      .cruiseControl(true)
      .sportsPack(true)
      .sunRoof(false)

    car.engine.capacity(2000)
      .numCylinders(4.asInstanceOf[Short])
      .putManufacturerCode(manufacturerCode, srcOffset)

    car.fuelFiguresCount(3).next.speed(30).mpg(35.9f)
      .next.speed(55).mpg(49.0f)
      .next.speed(75).mpg(40.0f)

    val perfFigures = car.performanceFiguresCount(2)
    perfFigures.next.octaneRating(95.asInstanceOf[Short])
      .accelerationCount(3).next.mph(30).seconds(4.0f)
      .next.mph(60).seconds(7.5f)
      .next.mph(100).seconds(12.2f)
    perfFigures.next.octaneRating(99.asInstanceOf[Short])
      .accelerationCount(3).next.mph(30).seconds(3.8f)
      .next.mph(60).seconds(7.1f)
      .next.mph(100).seconds(11.8f)

    car.putMake(make, srcOffset, make.length)
    car.putModel(model, srcOffset, model.length)

    car.size
  }

  private def decode(car: Car,
    directBuffer: DirectBuffer,
    bufferOffset: Int,
    actingBlockLength: Int,
    actingVersion: Int) {

    val buffer = Array.ofDim[Byte](128)
    val sb = new StringBuilder

    car.wrapForDecode(directBuffer, bufferOffset, actingBlockLength, actingVersion)

    sb.append(s"\ncar.templateId=${car.templateId}")
    sb.append(s"\ncar.serialNumber=${car.serialNumber}")
    sb.append(s"\ncar.modelYear=${car.modelYear}")
    sb.append(s"\ncar.available=${car.available}")
    sb.append(s"\ncar.code=${car.code}")

    sb.append("\ncar.someNumbers=")
    for (i <- 0 to (Car.someNumbersLength - 1))
      sb.append(car.someNumbers(i)).append(", ")

    sb.append("\ncar.vehicleCode=")
    for (i <- 0 to (Car.vehicleCodeLength - 1))
      sb.append((car.vehicleCode(i)).asInstanceOf[Char])

    val extras = car.extras
    sb.append(s"\ncar.extras.cruiseControl=${extras.cruiseControl}")
    sb.append(s"\ncar.extras.sportsPack=${extras.sportsPack}")
    sb.append(s"\ncar.extras.sunRoof=${extras.sunRoof}")

    val engine = car.engine
    sb.append(s"\ncar.engine.capacity=${engine.capacity}")
    sb.append(s"\ncar.engine.numCylinders=${engine.numCylinders}")
    sb.append(s"\ncar.engine.maxRpm=${engine.maxRpm}")
    sb.append("\ncar.engine.manufacturerCode=")
    for (i <- 0 to (Engine.manufacturerCodeLength - 1))
      sb.append((engine.manufacturerCode(i)).asInstanceOf[Char])

    sb.append(s"\ncar.engine.fuel=${new String(buffer, 0, engine.getFuel(buffer, 0, buffer.length), "ASCII")}")

    {
      // Code block to limit scope of implicit conversion, even though I'm using
      // a facility that forces me to be explicit about it (I'm OCD about this)
      import scala.collection.convert.decorateAsScala.asScalaIteratorConverter
      car.fuelFigures.asScala.foreach { fuelFigures =>
        sb.append(s"\ncar.fuelFigures.speed=${fuelFigures.speed}")
        sb.append(s"\ncar.fuelFigures.mpg=${fuelFigures.mpg}")
      }

      car.performanceFigures.asScala.foreach { performanceFigures =>
        sb.append(s"\ncar.performanceFigures.octaneRating=${performanceFigures.octaneRating}")
        performanceFigures.acceleration.asScala.foreach { acceleration =>
          sb.append("\ncar.performanceFigures.acceleration.mph=").append(acceleration.mph)
          sb.append("\ncar.performanceFigures.acceleration.seconds=").append(acceleration.seconds)
        }
      }
    }

    sb.append("\ncar.make=").append(
      new String(buffer, 0, car.getMake(buffer, 0, buffer.length), Car.makeCharacterEncoding))
    sb.append("\ncar.model=").append(
      new String(buffer, 0, car.getModel(buffer, 0, buffer.length), Car.modelCharacterEncoding))
    sb.append("\ncar.size=").append(car.size)

    println(sb)
  }
}
