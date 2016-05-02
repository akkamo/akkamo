package com.github.jurajburian.makka

import com.typesafe.config.Config

import scala.util.Try


/**
	* @author jubu
	*/
package object config {
	import scala.collection.JavaConversions._

	def blockAsMap(key:String)(implicit cfg:Config):Option[Map[String, Config]] = Try {

		val keys = cfg.getObject(key).keySet()
		keys.map{p=>(p, cfg.getConfig(s"$key.$p"))}.toMap.map(p=>(p._1, p._2.resolve()))
	}.toOption

	def getStringList(path:String)(implicit cfg:Config):Option[List[String]] =  {
		if(cfg.hasPath(path)) {
			Some(cfg.getStringList(path).toList)
		} else {
			None
		}
	}

	def getInt(path:String)(implicit cfg:Config):Option[Int] =  {
		if(cfg.hasPath(path)) {
			Some(cfg.getInt(path))
		} else {
			None
		}
	}

	def getString(path:String)(implicit cfg:Config):Option[String] =  {
		if(cfg.hasPath(path)) {
			Some(cfg.getString(path))
		} else {
			None
		}
	}

}
