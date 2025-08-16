package io.github.edadma.kit_cli

//import io.github.edadma.cross_platform.{processExit, processArgs}
//
//// Main entry point - delegates to CLI parser
//@main def run(args: String*): Unit = {
//  CliParser.parse(processArgs(args).toArray) match {
//    case Some(config) =>
//      CommandExecutor.execute(config)
//    case None =>
//      processExit(1)
//  }
//}

import sttp.client4.quick.*
import scala.util.{Try, Success, Failure}
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@main def testHttp(): Unit = {
  println("🌐 Testing sttp HTTP client...")
  println()

  // Test 1: Simple GET request
  testSimpleGet()

  // Test 2: JSON API request
  testJsonApi()

  // Test 3: Error handling
  testErrorHandling()
}

private def testSimpleGet(): Unit = {
  println("📡 Test 1: Simple GET request")
  try {
    val response = quickRequest
      .get(uri"https://httpbin.org/get")
      .send(backend)

    println(s"✅ Status: ${response.code}")
    println(s"✅ Headers: ${response.headers.size} headers")
    println(s"✅ Body length: ${response.body.length} characters")

    // Show first 200 chars of response
    val preview = if (response.body.length > 200) {
      response.body.take(200) + "..."
    } else {
      response.body
    }
    println(s"📄 Response preview:\n${preview}")

  } catch {
    case e: Exception =>
      println(s"❌ Error: ${e.getMessage}")
  }
  println()
}

private def testJsonApi(): Unit = {
  println("🔍 Test 2: JSON API request")
  try {
    val response = quickRequest
      .get(uri"https://jsonplaceholder.typicode.com/posts/1")
      .send(backend)

    println(s"✅ Status: ${response.code}")
    println(s"📄 JSON Response:\n${response.body}")

    // Simple JSON parsing check
    if (response.body.contains("\"userId\"")) {
      println("✅ JSON structure looks correct")
    } else {
      println("⚠️ Unexpected JSON structure")
    }

  } catch {
    case e: Exception =>
      println(s"❌ Error: ${e.getMessage}")
  }
  println()
}

private def testErrorHandling(): Unit = {
  println("⚠️ Test 3: Error handling")

  // Test 404 error
  try {
    val response = quickRequest
      .get(uri"https://httpbin.org/status/404")
      .send(backend)

    println(s"📡 404 test - Status: ${response.code}")
    if (response.code.code == 404) {
      println("✅ 404 handling works correctly")
    } else {
      println("⚠️ Unexpected status code")
    }

  } catch {
    case e: Exception =>
      println(s"❌ Error handling test failed: ${e.getMessage}")
  }

  // Test invalid URL
  println()
  println("🔗 Testing invalid URL...")
  Try {
    val response = quickRequest
      .get(uri"https://this-domain-definitely-does-not-exist-12345.com")
      .send(backend)
    response
  } match {
    case Success(response) =>
      println(s"⚠️ Unexpected success: ${response.code}")
    case Failure(exception) =>
      println(s"✅ Correctly caught network error: ${exception.getClass.getSimpleName}")
  }

  println()
}

// Helper method for future kit commands
def makeGetRequest(url: String): Try[String] = {
  Try {
    val response = quickRequest
      .get(uri"$url")
      .send(backend)

    if (response.code.isSuccess) {
      response.body
    } else {
      throw new RuntimeException(s"HTTP ${response.code.code}: ${response.statusText}")
    }
  }
}
