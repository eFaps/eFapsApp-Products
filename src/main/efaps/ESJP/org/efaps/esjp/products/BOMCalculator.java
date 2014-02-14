/*
 * Copyright 2003 - 2012 The eFaps Team
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
 * Revision:        $Rev: 8342 $
 * Last Changed:    $Date: 2012-12-11 09:42:17 -0500 (Tue, 11 Dec 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */


package org.efaps.esjp.products;

import java.math.BigDecimal;

import org.efaps.admin.event.Parameter;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;


public class BOMCalculator
    extends BOMCalculator_Base
{
    public BOMCalculator(final Parameter _parameter,
                         final Instance _prodInst,
                         final BigDecimal _quantityRequired)
        throws EFapsException
    {
        super(_parameter, _prodInst, _quantityRequired);
        // TODO Auto-generated constructor stub
    }

}
