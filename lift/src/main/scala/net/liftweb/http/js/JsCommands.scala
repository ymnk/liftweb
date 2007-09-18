package net.liftweb.http.js

/*                                                *\
 (c) 2007 WorldWide Conferencing, LLC
 Distributed under an Apache License
 http://www.apache.org/licenses/LICENSE-2.0
 \*                                                 */

import scala.xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.util._

object JsCommands {
  def create = new JsCommands(Nil)  
  def apply(in: Seq[JsCmd]) = new JsCommands(in.toList.reverse)
}

class JsCommands(val reverseList: List[JsCmd]) extends ResponseIt {
  def +(in: JsCmd) = new JsCommands(in :: reverseList)
  def +(in: List[JsCmd]) = new JsCommands(in.reverse ::: reverseList)
  
  def toResponse = {
    val data = reverseList.reverse.map(_.toJsCmd).mkString("\n").getBytes("UTF-8")
    Response(data, List("Content-Length" -> data.length.toString, "Content-Type" -> "text/javascript"), 200)
  }
}

abstract class JsCmd {
  def +(other: JsCmd): JsCmd = JsCmds.CmdPair(this, other)  
  def toJsCmd: String
}

object JsCmds {
  class JsEscaper(val in: String) {
    def esc = "'"+doEsc(in.toList).mkString("")+"'"
    def doEsc(what: List[Char]): List[Char] = what match {
      case Nil => Nil
      case '\\' :: xs => escChar('\\') ::: doEsc(xs)
      case '\'' :: xs => escChar('\'') ::: doEsc(xs)
      case n :: xs if n < ' ' => escChar(n) ::: doEsc(xs)
      case n :: xs if n > '~' => escChar(n) ::: doEsc(xs)
      case n :: xs => n :: doEsc(xs)
    }
    
    def escChar(in: Char): List[Char] = '\\' :: 'u' :: (
      Integer.toString(in.toInt, 16).toList match {
        case x :: Nil => '0' :: '0' :: '0' :: x :: Nil
        case x1 :: x2 :: Nil => '0' :: '0' :: x1 :: x2 :: Nil
        case x1 :: x2 :: x3 :: Nil => '0' :: x1 :: x2 :: x3 :: Nil
        case xs => xs
      }
    )
  }
  
  implicit def strToJsEsc(in: String): JsEscaper = new JsEscaper(in)
  
  case class CmdPair(left: JsCmd, right: JsCmd) extends JsCmd {
    def toJsCmd = left.toJsCmd + "\n" + right.toJsCmd
  }
  case class Set(uid: String, content: NodeSeq) extends JsCmd {
    def toJsCmd = {
      val html = S.request.fixHtml(content).toString 
      val ret = "try{jQuery("+("#"+uid).esc+").each(function(i) {this.innerHTML = "+html.esc+";});} catch (e) {}"
      ret
    }
  }

  trait HasTime {
    def time: Can[Int]
    
    def timeStr = time.map(_.toString) openOr ""
  }

  case class Show(uid: String, time: Can[Int]) extends JsCmd with HasTime {
    def this(uid: String) = this(uid, Empty)
    def this(uid: String, time: Int) = this(uid, Full(time))
    
    def toJsCmd = "try{jQuery("+("#"+uid).esc+").show("+timeStr+");} catch (e) {}"
    
  }

  case class Hide(uid: String, time: Can[Int]) extends JsCmd with HasTime {
    def this(uid: String) = this(uid, Empty)
    def this(uid: String, time: Int) = this(uid, Full(time))
    
    def toJsCmd = "try{jQuery("+("#"+uid).esc+").hide("+timeStr+");} catch (e) {}"
  }

  case class Alert(text: String) extends JsCmd {
    def toJsCmd = "alert("+text.esc+");"
  }

  case class Run(text: String) extends JsCmd {
    def toJsCmd = text
  }

  case class Noop extends JsCmd {
    def toJsCmd = ""
  }
}