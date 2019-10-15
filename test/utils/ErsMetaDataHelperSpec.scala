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

package utils

import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec

class ErsMetaDataHelperSpec extends UnitSpec with MockitoSugar with Matchers with GuiceOneAppPerSuite {
  def injector: Injector = app.injector
  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
  override def fakeApplication() = new GuiceApplicationBuilder().configure(Map("play.i18n.langs"->List("en-GB","en","cy-GB", "cy"))).build()

  "ErsMetaDataHelper" should {
    "rewrite a schmemeInfo String from english to welsh" in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("cy").get))

      val schemeInfo = "CSOP - CSOP - XA1100000000000 - 2015 to 2016"
      ErsMetaDataHelper.rewriteSchemeInfo(schemeInfo) should be ("CSOP - CSOP - XA1100000000000 - 2015 i 2016")
    }

    "rewrite a schmemeInfo String from english to english" in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))

      val schemeInfo = "CSOP - CSOP - XA1100000000000 - 2015 to 2016"
      ErsMetaDataHelper.rewriteSchemeInfo(schemeInfo) should be ("CSOP - CSOP - XA1100000000000 - 2015 to 2016")
    }

    "rewrite a schemeInfo string from welsh to english" in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))

      val schemeInfo = "CSOP - CSOP - XA1100000000000 - 2015 i 2016"

      ErsMetaDataHelper.rewriteSchemeInfo(schemeInfo) should be ("CSOP - CSOP - XA1100000000000 - 2015 to 2016")

    }

    "return the same string if no match " in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))
      val schemeInfo = "CSOP - CSOP - XA1100000000000 - 2015 by 2016"

      ErsMetaDataHelper.rewriteSchemeInfo(schemeInfo) should be (schemeInfo)
    }
  }
}