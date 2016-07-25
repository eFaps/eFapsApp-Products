/*
 * Copyright 2003 - 2016 The eFaps Team
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
 *
 */


package org.efaps.esjp.products.dashboard;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("423d07bd-1d05-428a-8491-f9d30b0f5c86")
@EFapsApplication("eFapsApp-Products")
public class InventoryValuePanel
    extends InventoryValuePanel_Base
{
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new inventory value panel_ base.
     */
    public InventoryValuePanel()
    {
        super();
    }

    /**
     * Instantiates a new inventory value panel_ base.
     *
     * @param _config the _config
     */
    public InventoryValuePanel(final String _config)
    {
        super(_config);
    }
}
