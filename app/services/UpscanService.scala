/*
 * Copyright 2019 HM Revenue & Customs
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

package services

import com.google.inject.Inject
import connectors.UpscanConnector
import models.upscan.{UploadId, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class UpscanService @Inject()(
                             upscanConnector: UpscanConnector
                             ) {

  def getUpscanFormDataCsv(uploadId: UploadId, scRef: String)(implicit hc: HeaderCarrier, request: Request[AnyRef]): Future[UpscanInitiateResponse] = {
    val callback = controllers.routes.CsvFileUploadCallbackController.callback(uploadId, scRef).absoluteURL()

    val success = controllers.routes.CsvFileUploadController.success(uploadId).absoluteURL()
    val failure = controllers.routes.CsvFileUploadController.failure().absoluteURL()
    val upscanInitiateRequest = UpscanInitiateRequest(callback, success, failure)
    upscanConnector.getUpscanFormData(upscanInitiateRequest)
  }

  def getUpscanFormDataOds()(implicit hc: HeaderCarrier, request: Request[_]): Future[UpscanInitiateResponse] = {
    val callback = controllers.routes.FileUploadCallbackController.callback(hc.sessionId.get.value).absoluteURL()

    val success = controllers.routes.FileUploadController.success().absoluteURL()
    val failure = controllers.routes.FileUploadController.failure().absoluteURL()
    val upscanInitiateRequest = UpscanInitiateRequest(callback, success, failure)
    upscanConnector.getUpscanFormData(upscanInitiateRequest)
  }

}