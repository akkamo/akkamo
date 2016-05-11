package com.github.jurajburian.makka

import com.typesafe.config.Config

import scala.util.Try


/**
	* @author jubu
	*/
package object config {

	import scala.collection.JavaConversions._

	def blockAsMap(key: String)(implicit cfg: Config): Option[Map[String, Config]] = Try {

		val keys = cfg.getObject(key).keySet()
		keys.map { p => (p, cfg.getConfig(s"$key.$p")) }.toMap.map(p => (p._1, p._2.resolve()))
	}.toOption

	def getStringList(path: String)(implicit cfg: Config): Option[List[String]] = {
		if (cfg.hasPath(path)) {
			Some(cfg.getStringList(path).toList)
		} else {
			None
		}
	}

	def getInt(path: String)(implicit cfg: Config): Option[Int] = {
		if (cfg.hasPath(path)) {
			Some(cfg.getInt(path))
		} else {
			None
		}
	}

	def getString(path: String)(implicit cfg: Config): Option[String] = {
		if (cfg.hasPath(path)) {
			Some(cfg.getString(path))
		} else {
			None
		}
	}

	def getBoolean(path: String)(implicit cfg: Config): Option[Boolean] = {
		if (cfg.hasPath(path)) {
			Some(cfg.getBoolean(path))
		} else {
			None
		}
	}

	private def getInternal[T](path: String, cfg: Config, t: Transformer[T]): Option[T] = {
		if (cfg.hasPath(path)) {
			Some(t.transform(cfg, path))
		} else {
			None
		}
	}

	def get[T](key: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = {
		getInternal[T](key, cfg, t)
	}


	def get[T](key: String)(implicit t: Transformer[T], cfg: Config): Option[T] = {
		getInternal[T](key, cfg, t)
	}

	implicit val cfg2Int = new Transformer[Int] {
		override def transform(cfg: Config, key: String): Int = cfg.getInt(key)
	}

	implicit val cfg2String = new Transformer[String] {
		override def transform(cfg: Config, key: String): String = cfg.getString(key)
	}

	implicit val cfg2StringList = new Transformer[List[String]] {
		override def transform(cfg: Config, key: String): List[String] = cfg.getStringList(key).toList
	}

	implicit val cfg2Config = new Transformer[Config] {
		override def transform(cfg: Config, key: String): Config = cfg.getConfig(key)
	}

	implicit val cfg2Boolean = new Transformer[Boolean] {
		override def transform(cfg: Config, key: String): Boolean = cfg.getBoolean(key)
	}
}

trait Transformer[T] {
	def transform(cfg: Config, key: String): T
}
