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

	private def getInternal[T](path: String, cfg: Config, t: Transformer[T]): Option[T] = {
		if (cfg.hasPath(path)) {
			Some(t(cfg, path))
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

	type Transformer[T] = (Config, String) => T

	implicit val cfg2Int: Transformer[Int] = (cfg: Config, key: String) => cfg.getInt(key)

	implicit val cfg2String: Transformer[String] = (cfg: Config, key: String) => cfg.getString(key)

	implicit val cfg2StringList: Transformer[List[String]] = (cfg: Config, key: String) => cfg.getStringList(key).toList

	implicit val cfg2Config: Transformer[Config] = (cfg: Config, key: String) => cfg.getConfig(key)

	implicit val cfg2Boolean: Transformer[Boolean] = (cfg: Config, key: String) => cfg.getBoolean(key)
}



