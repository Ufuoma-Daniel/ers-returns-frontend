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

import models._
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{CacheUtil, PageBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object CheckCsvFilesController extends CheckCsvFilesController {
  override val cacheUtil: CacheUtil = CacheUtil
  override val pageBuilder: PageBuilder = PageBuilder
}

trait CheckCsvFilesController extends ERSReturnBaseController with Authenticator {
  val cacheUtil: CacheUtil
  val pageBuilder: PageBuilder

  def checkCsvFilesPage(): Action[AnyContent] = AuthorisedForAsync() {
    implicit user =>
      implicit request =>
        showCheckCsvFilesPage()(user, request, hc)
  }

  def showCheckCsvFilesPage()(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {

    (for {
      requestObject <- cacheUtil.fetch[RequestObject](cacheUtil.ersRequestObject)
      cacheData     <- cacheUtil.fetchOption[CsvFilesCallbackList](CacheUtil.CHECK_CSV_FILES, requestObject.getSchemeReference).recover{
        case _: NoSuchElementException => None
      }
    } yield {

      val csvFilesList: List[CsvFiles] = PageBuilder.getCsvFilesList(requestObject.getSchemeType)

      cacheData match {

        case Some(data) =>
          val mergeWithSelected: List[CsvFiles] = mergeCsvFilesListWithCsvFilesCallback(csvFilesList, data)

          Ok(views.html.check_csv_file(requestObject, CsvFilesList(mergeWithSelected)))

        case None =>
          Ok(views.html.check_csv_file(requestObject, CsvFilesList(csvFilesList)))
      }
    })
    .recover {
      case _: Throwable => getGlobalErrorPage
    }
  }

  def mergeCsvFilesListWithCsvFilesCallback(csvFilesList: List[CsvFiles], cacheData: CsvFilesCallbackList): List[CsvFiles] = {
    for (file <- csvFilesList) yield {
      if (cacheData.files.exists(_.fileId == file.fileId)) {
        CsvFiles(file.fileId, Some(PageBuilder.OPTION_YES))
      }
      else {
        file
      }
    }
  }

  def checkCsvFilesPageSelected(): Action[AnyContent] = AuthorisedForAsync() {
    implicit user =>
      implicit request =>
        validateCsvFilesPageSelected()
  }

  def validateCsvFilesPageSelected()(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    RsFormMappings.csvFileCheckForm.bindFromRequest.fold(
      formWithErrors => {
        reloadWithError()
      },
      formData => {
        performCsvFilesPageSelected(formData)
      }
    )
  }

  def performCsvFilesPageSelected(formData: CsvFilesList)(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {

    val csvFilesCallbackList: List[CsvFilesCallback] = createCacheData(formData.files)
    if (csvFilesCallbackList.length == 0) {
      reloadWithError()
    }
    else {

      (for{
        requestObject <- cacheUtil.fetch[RequestObject](cacheUtil.ersRequestObject)
        _             <- cacheUtil.cache(CacheUtil.CHECK_CSV_FILES, CsvFilesCallbackList(csvFilesCallbackList), requestObject.getSchemeReference)
      } yield {

        Redirect(routes.CsvFileUploadController.uploadFilePage())
      }).recover {
        case e: Throwable => {
          Logger.error(s"checkCsvFilesPageSelected: Save data to cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
          getGlobalErrorPage
        }
      }
    }
  }

  def createCacheData(csvFilesList: List[CsvFiles]): List[CsvFilesCallback] = {
    for (fileData <- csvFilesList if fileData.isSelected.getOrElse("") == PageBuilder.OPTION_YES) yield {
      CsvFilesCallback(fileData.fileId, None)
    }
  }

  def reloadWithError()(implicit messages: Messages): Future[Result] = {
    Future.successful(
      Redirect(routes.CheckCsvFilesController.checkCsvFilesPage()).flashing("csv-file-not-selected-error" -> messages(PageBuilder.PAGE_CHECK_CSV_FILE + ".err.message"))
    )
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages) = Ok(views.html.global_error(
    messages("ers.global_errors.title"),
    messages("ers.global_errors.heading"),
    messages("ers.global_errors.message"))(request, messages))

}
