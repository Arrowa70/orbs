package org.seekloud.orbs.shared.ptcl.model

import scala.collection.mutable

/**
  * User: TangYaruo
  * Date: 2018/11/14
  * Time: 14:34
  */
object Constants {

  object GameState{
    val firstCome = 1
    val play = 2
    val stop = 3
    val loadingPlay = 4
    val relive = 5
    val replayLoading = 6
  }

  val preExecuteFrameOffset = 2 //预执行2帧

  val canvasUnitPerLine = 120 //可视窗口每行显示多少个canvasUnit


}