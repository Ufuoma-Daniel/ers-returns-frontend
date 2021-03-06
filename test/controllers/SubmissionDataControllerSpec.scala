/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import akka.stream.Materializer
import connectors.ErsConnector
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import utils.{ERSFakeApplicationConfig, Fixtures}

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

class SubmissionDataControllerSpec extends UnitSpec with ERSFakeApplicationConfig with MockitoSugar with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer
  implicit lazy val messages: Messages = Messages(Lang("en"), app.injector.instanceOf[MessagesApi])

  "calling createSchemeInfoFromURL" should {

    val submissionDataController: SubmissionDataController = new SubmissionDataController {
      override val ersConnector: ErsConnector = mock[ErsConnector]
    }

    "return correct json if all parameters are given in request" in {
      val request = FakeRequest("GET", "/get-submission-data?schemeRef=AA0000000000000&confTime=2016-08-05T11:14:30")
      val result = submissionDataController.createSchemeInfoFromURL(request)
      result shouldBe Some(
        Json.obj(
          "schemeRef" -> "AA0000000000000",
          "confTime" -> "2016-08-05T11:14:30"
        )
      )
    }

    "return None if not all parameters are given in request" in {
      val request = FakeRequest("GET", "").withBody(
        Json.obj(
          "confTime" -> "2016-08-05T11:14:30"
        )
      )
      val result = submissionDataController.createSchemeInfoFromURL(request)
      result shouldBe None
    }

  }

  "calling retrieveSubmissionData" should {
    val submissionDataController: SubmissionDataController = new SubmissionDataController {
      override val ersConnector: ErsConnector = mock[ErsConnector]
    }

    "redirect to login page if user is not authenticated" in {
      val result = submissionDataController.retrieveSubmissionData()(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe SEE_OTHER
    }
  }

  "calling getRetrieveSubmissionData" should {

    val mockErsConnector: ErsConnector = mock[ErsConnector]

    "returns NOT_FOUND if not all parameters are given" in {
      reset(mockErsConnector)
      val submissionDataController: SubmissionDataController = new SubmissionDataController {
        override val ersConnector: ErsConnector = mockErsConnector

        override def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] = None
      }
      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe NOT_FOUND
    }

    "returns OK if all parameters are given and retrieveSubmissionData is successful" in {
      reset(mockErsConnector)
      val submissionDataController: SubmissionDataController = new SubmissionDataController {
        override val ersConnector: ErsConnector = mockErsConnector

        override def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] = Some(mock[JsObject])
      }
      when(
        mockErsConnector.retrieveSubmissionData(any[JsObject]())(any(), any())
      ).thenReturn(
        Future.successful(HttpResponse(OK, Some(Json.obj())))
      )
      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe OK
      bodyOf(result).contains("Retrieve Failure") shouldBe false
    }

    "shows error page if all parameters are given but retrieveSubmissionData fails" in {
      reset(mockErsConnector)
      val submissionDataController: SubmissionDataController = new SubmissionDataController {
        override val ersConnector: ErsConnector = mockErsConnector

        override def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] = Some(mock[JsObject])
      }
      when(
        mockErsConnector.retrieveSubmissionData(any[JsObject]())(any(), any())
      ).thenReturn(
        Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))
      )
      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe OK
      bodyOf(result).contains(messages("ers.global_errors.message")) shouldBe true
    }

    "shows error page if all parameters are given but retrieveSubmissionData throws exception" in {
      reset(mockErsConnector)
      val submissionDataController: SubmissionDataController = new SubmissionDataController {
        override val ersConnector: ErsConnector = mockErsConnector

        override def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] = Some(mock[JsObject])
      }
      when(
        mockErsConnector.retrieveSubmissionData(any[JsObject]())(any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe OK
      bodyOf(result).contains(messages("ers.global_errors.message")) shouldBe true
    }

  }


}
