/**
  * Copyright (C) 2017 Orbeon, Inc.
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the
  * GNU Lesser General Public License as published by the Free Software Foundation; either version
  *  2.1 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
  */
package org.orbeon.oxf.xforms

import javax.servlet.{ServletContextEvent, ServletContextListener}

import org.orbeon.oxf.cache
import org.orbeon.oxf.common.Version
import org.orbeon.oxf.util.SLF4JLogging._

class ReplicationServletContextListener extends ServletContextListener {

  def contextInitialized(servletContextEvent: ServletContextEvent): Unit =
    if (XFormsProperties.isReplication) {
      Version.instance.requirePEFeature("State replication")
      withDebug("eagerly bootstrapping caches for replication")(Caches)(cache.Caches.Logger)
    }

  def contextDestroyed(servletContextEvent: ServletContextEvent) = ()
}