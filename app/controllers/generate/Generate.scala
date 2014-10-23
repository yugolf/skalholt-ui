package controllers.generate

import controllers.common.CommonController
import forms.generate.{ GenerateForm, GenerateForms }
import java.io.File
import java.lang.Exception
import skalholt.codegen.constants.GenConstants.GenParam
import skalholt.codegen.database.common.DBUtils
import skalholt.codegen.main.{ Generate => Skalholt }
import play.api.Logger
import play.api.mvc._
import play.cache.Cache
import scala.slick.ast.ColumnOption.PrimaryKey

object Generate extends CommonController with GenerateForm {

  val separator = System.getProperty("file.separator")
  def index = CommonAction { implicit request =>

    val currentDir = new File(".").getAbsoluteFile().getParent().replace(s"${separator}skalholt${separator}bin", "")
    val form = Cache.get("genparam") match {
      case p: GenParam =>
        GenerateForms(
          p.bizSlickDriver,
          p.bizJdbcDriver,
          p.bizUrl,
          p.bizUser,
          p.bizPassword,
          p.bizSchema,
          p.outputFolder.getOrElse(currentDir),
          p.pkg,
          p.ignoreTables)
      case _ =>
        GenerateForms(None, None, None, None, None, None, currentDir, None, None)
    }

    Ok(views.html.generate.generate(generateForm.fill(form)))
  }

  def generateAll = CommonAction { implicit request =>
    generateForm.bindFromRequest.fold(
      hasErrors = { form =>
        Ok(views.html.generate.generate(form))
      },
      success = { form =>
        val pkg = Some("models")
        val password = form.password match {
          case None => Some("")
          case x => x
        }
        val param = GenParam(form.slickDriver, form.jdbcDriver, form.url, form.user, password,
          form.schema, None, Some(form.outputFolder), pkg, None)
        Logger.debug("param=" + param)

        try {
          val model = DBUtils.getModel(form.jdbcDriver.get, form.url.get, form.schema.get, form.user, password)
          if (model.tables.length <= 0) {
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("No tables.").bindFromRequest))
          } else {
            val primaryKey =  model.tables.head.columns.exists(_.options.contains(PrimaryKey))
            if (primaryKey == false)
                BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Primary key does not exist in the table").bindFromRequest))
            else
            try {

              Cache.set("genparam", param)
              Skalholt.all(param)
              Redirect(controllers.generate.routes.Generate.index)
                .flashing("success" -> "You have now generated the source code.")
            } catch {
              case e: Exception =>
                BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Generator failure.").bindFromRequest))
            }
          }
        } catch {
          case e: Exception =>
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Database connection error.").bindFromRequest))
        }

      })
  }

  def importData = CommonAction { implicit request =>
    generateForm.bindFromRequest.fold(
      hasErrors = { form =>
        Ok(views.html.generate.generate(form))
      },
      success = { form =>
        val pkg = Some("models")
        val password = form.password match {
          case None => Some("")
          case x => x
        }
        val param = GenParam(form.slickDriver, form.jdbcDriver, form.url, form.user, password,
          form.schema, None, Some(form.outputFolder), pkg, None)

        try {
          val model = DBUtils.getModel(form.jdbcDriver.get, form.url.get, form.schema.get, form.user, form.password)
          if (model.tables.length <= 0) {
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("No tables.").bindFromRequest))
          } else {
            try {

              Cache.set("genparam", param)
              Skalholt.importData(param)
              Redirect(controllers.generate.routes.Generate.index)
                .flashing("success" -> "You have now imported database schema.")
            } catch {
              case e: Exception =>
                BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Generator failure.").bindFromRequest))
            }
          }
        } catch {
          case e: Exception =>
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Database connection error.").bindFromRequest))
        }

      })
  }

  def generate = CommonAction { implicit request =>
    generateForm.bindFromRequest.fold(
      hasErrors = { form =>
        Ok(views.html.generate.generate(form))
      },
      success = { form =>
        val pkg = Some("models")
        val param = GenParam(form.slickDriver, form.jdbcDriver, form.url, form.user, form.password,
          form.schema, None, Some(form.outputFolder), pkg, None)

        try {
          val model = DBUtils.getModel(form.jdbcDriver.get, form.url.get, form.schema.get, form.user, form.password)
          if (model.tables.length <= 0) {
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("No tables.").bindFromRequest))
          } else {
            try {

              Cache.set("genparam", param)
              Skalholt.generate(param)
              Redirect(controllers.generate.routes.Generate.index)
                .flashing("success" -> "You have now imported database schema and generated the source code.")
            } catch {
              case e: Exception =>
                BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Generator failure.").bindFromRequest))
            }
          }
        } catch {
          case e: Exception =>
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Database connection error.").bindFromRequest))
        }

      })
  }

  def regenerate = CommonAction { implicit request =>

    Cache.get("genparam") match {
      case p: GenParam =>
        val form = GenerateForms(
          p.bizSlickDriver,
          p.bizJdbcDriver,
          p.bizUrl,
          p.bizUser,
          p.bizPassword,
          p.bizSchema,
          p.outputFolder.get,
          p.pkg,
          p.ignoreTables)
        val param = GenParam(form.slickDriver, form.jdbcDriver, form.url, form.user, form.password,
          form.schema, None, Some(form.outputFolder), p.pkg, None)

        try {
          val model = DBUtils.getModel(form.jdbcDriver.get, form.url.get, form.schema.get, form.user, form.password)
          if (model.tables.length <= 0) {
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("No tables.").bindFromRequest))
          } else {
            try {
              Skalholt.generate(param)
              Redirect(controllers.generate.routes.Generate.index)
                .flashing("success" -> "You have now imported database schema and generated the source code.")
            } catch {
              case e: Exception =>
                BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Generator failure.").bindFromRequest))
            }
          }
        } catch {
          case e: Exception =>
            BadRequest(views.html.generate.generate(generateForm.fill(form).withGlobalError("Database connection error.").bindFromRequest))
        }
      case _ =>
        BadRequest(views.html.generate.generate(generateForm.withGlobalError("Code generate error.").bindFromRequest))
    }

  }
}