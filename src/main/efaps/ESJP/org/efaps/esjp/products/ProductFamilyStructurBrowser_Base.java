/*
 * Copyright 2003 - 2015 The eFaps Team
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

package org.efaps.esjp.products;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.ci.CITableProducts;
import org.efaps.esjp.ui.structurbrowser.StandartStructurBrowser;
import org.efaps.ui.wicket.models.objects.UIStructurBrowser;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO description!
 *
 * @author The eFasp Team
 */
@EFapsUUID("5b20a9b1-a960-4441-ba00-3c35585a9108")
@EFapsApplication("eFapsApp-Products")
public abstract class ProductFamilyStructurBrowser_Base
    extends StandartStructurBrowser
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProductFamilyStructurBrowser.class);


    @Override
    protected Return checkHideColumn4Row(final Parameter _parameter)
        throws EFapsException
    {
        return new Return();
    }

    /**
     * @param _parameter Paraemter as passed from the eFasp API
     * @return Comparable value
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected Comparable getComparable(final Parameter _parameter,
                                       final UIStructurBrowser _structurBrowser)
    {
        final StringBuilder ret = new StringBuilder();
        UIStructurBrowser brwsr = _structurBrowser;
        while(!brwsr.isRoot()) {
            brwsr = brwsr.getParentBrws();
        }
        if (CITableProducts.Products_ProductFamilyTable.codePart.name.equals(brwsr.getSortKey())) {
            try {
                ret.append(new ProductFamily().getCode(_parameter, _structurBrowser.getInstance()));
            } catch (final EFapsException e) {
                LOG.error("Catched error on evaluation of Comparable", e);
            }
        } else {
            ret.append(_structurBrowser.getLabel());
        }
        return ret.toString();
    }

}
