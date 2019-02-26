package org.seekloud.orbs.front.orbs

import org.scalajs.dom
import org.scalajs.dom.raw.Event
import org.seekloud.orbs.front.utils.{JsFunc, Shortcut}
import org.seekloud.orbs.front.utils.middleware.MiddleFrameInJs
import org.seekloud.orbs.shared.ptcl.config.OrbsConfigImpl
import org.seekloud.orbs.shared.ptcl.model.Constants.GameState
import org.seekloud.orbs.shared.ptcl.model.{Constants, Point}
import org.seekloud.orbs.shared.ptcl.protocol.OrbsProtocol
import org.seekloud.orbs.shared.ptcl.protocol.OrbsProtocol.{UserMapReq, WsMsgServer}

/**
  * User: TangYaruo
  * Date: 2019/2/6
  * Time: 14:14
  */
abstract class GameHolder(canvasName: String, opCanvasName: String) extends NetworkInfo {

  println(s"GameHolder $canvasName...")

  val drawFrame = new MiddleFrameInJs

  /*canvas*/
  protected var canvasWidth = Constants.Canvas.width
  protected var canvasHeight = Constants.Canvas.height
  protected val myCanvas = drawFrame.createCanvas(canvasName, canvasWidth, canvasHeight)
  protected val myCtx = myCanvas.getCtx
  protected val opCanvas = drawFrame.createCanvas(opCanvasName, canvasWidth, canvasHeight)
  protected val opCtx = opCanvas.getCtx

  protected var canvasBoundary = Point(Constants.Canvas.width, Constants.Canvas.height)
  protected var canvasUnit = canvasWidth / Constants.canvasUnitPerLine
  protected var canvasBounds = canvasBoundary / canvasUnit

  var orbsSchemaOpt: Option[OrbsSchemaClientImpl] = None

  var gameState: Int = GameState.firstCome


  protected var myId = ""
  protected var myName = ""
  protected var opId: Option[String] = None
  protected var opName: Option[String] = None
  protected var myByteId: Byte = 0
  protected var opByteId: Option[Byte] = None
  protected var opLeft = false
  protected var gameConfig: Option[OrbsConfigImpl] = None
  protected var firstCome = true

  protected val webSocketClient = new WebSocketClient(wsConnectSuccess, wsConnectError, wsMessageHandler, wsConnectClose)

  var justSynced = false

  protected var timer: Int = 0
  protected var nextFrame = 0
  protected var logicFrameTime: Long = System.currentTimeMillis()

  /*弹幕*/
  protected var barrage: Option[(Byte, String)] = None //(sender, info)
  protected var barrageTime = 0

  protected var normalB: Option[String] = None
  protected var normalBT = 0

  def gameRender(): Double => Unit = { d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    drawGameByTime(offsetTime, canvasUnit, canvasBounds)
    //    if(gameState == GameState.stop && orbsSchemaOpt.nonEmpty) orbsSchemaOpt.foreach(_.drawGameStop(killerName, killNum, energyScore, level))
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }

  protected def handleResize(): Unit = {
    val width = dom.window.innerWidth.toFloat
    val height = dom.window.innerHeight.toFloat
    val perLine = Constants.canvasUnitPerLine
    if (width != canvasWidth || height != canvasHeight) {
      canvasWidth = width
      canvasHeight = height
      canvasUnit = canvasWidth / perLine
      canvasBoundary = Point(canvasWidth, canvasHeight)
      canvasBounds = canvasBoundary / canvasUnit
      myCanvas.setWidth(canvasWidth)
      myCanvas.setHeight(canvasHeight)
      orbsSchemaOpt.foreach(_.updateSize(canvasBoundary, canvasUnit))
    }
  }

  protected def wsConnectSuccess(e: Event): Event = {
    println(s"连接服务器成功")
    e
  }


  protected def wsConnectError(e: Event): Event = {
    JsFunc.alert("网络连接错误，请重新刷新")
    e
  }


  protected def wsConnectClose(e: Event): Event = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  protected def wsMessageHandler(e: WsMsgServer)


  def closeHolder(): Unit = {
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    webSocketClient.closeWs
  }


  protected def sendMsg2Server(msg: OrbsProtocol.WsMsgFront): Unit = {
    webSocketClient.sendMsg(msg)
  }

  var lastSendReq = 0L

  protected def gameLoop(): Unit = {
    //    handleResize()
//    println(s"gameState: $gameState")
    logicFrameTime = System.currentTimeMillis()
    gameState match {
      case GameState.firstCome =>
        orbsSchemaOpt.foreach {
          _.drawGameLoading(myCtx)
        }
      case GameState.wait4Opponent =>
        orbsSchemaOpt.foreach(_.drawWaitingOp(myCtx))
        orbsSchemaOpt.foreach(_.drawOpNotIn(opCtx))

      case GameState.loadingPlay =>
        orbsSchemaOpt.foreach {
          _.drawGameLoading(myCtx)
        }

      case GameState.play =>
        orbsSchemaOpt.foreach { orbsSchema =>
          orbsSchema.update()
          if (orbsSchema.needUserMap && logicFrameTime - lastSendReq > 5000) {
            println("request for user map")
            webSocketClient.sendMsg(UserMapReq)
            lastSendReq = System.currentTimeMillis()
          }
        }
        logicFrameTime = System.currentTimeMillis()
//        ping()

      case GameState.stop =>
      //TODO
        orbsSchemaOpt.foreach { orbsSchema =>
          if (orbsSchema.episodeWinner.isEmpty) {
            orbsSchema.update()
          }
        }
      case GameState.wait4Relive =>
        orbsSchemaOpt.foreach(_.drawWait4Relive(myCtx))
        orbsSchemaOpt.foreach(_.drawOpRestart(opCtx))

    }
  }

  def drawGameByTime(offsetTime: Long, canvasUnit: Float, canvasBounds: Point): Unit = {
    orbsSchemaOpt.foreach { orbsSchema =>
      if (orbsSchema.plankMap.contains(myId)) {
        if (gameState != GameState.wait4Opponent && gameState != GameState.wait4Relive) {
          orbsSchema.drawGame(offsetTime, canvasUnit, canvasBounds)
          if (barrage.nonEmpty && barrageTime > 0) {
            orbsSchema.drawBarrage(barrage.get._1, barrage.get._2)
            barrageTime -= 1
          }
          if (normalB.nonEmpty && normalBT > 0) {
            orbsSchema.drawNormalBarrage(normalB.get)
            normalBT -= 1
          }
        }
      } else {
        if (gameState != GameState.wait4Opponent) {
          orbsSchema.drawGameLoading(myCtx)
        }
      }
    }
  }


}
