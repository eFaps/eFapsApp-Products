/*
 * Copyright 2003 - 2010 The eFaps Team
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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */


package org.efaps.esjp.products;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f1674c5a-9a87-4b3c-8d49-34172bee5cee")
@EFapsRevision("$Rev$")
public abstract class TreeView_Base
{
    /**
     * Method is used for an autocomplete field in the form for transactions.
     *
     * @param _parameter parameter from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return autoComplete4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String types = (String) properties.get("Types");
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> orderMap = new TreeMap<String, Map<String, String>>();
        if(input.length() > 0) {
            final boolean nameSearch = Character.isDigit(input.charAt(0));
            final QueryBuilder queryBldr = (types == null) ? new QueryBuilder(CIProducts.ProductAbstract)
                                                     : new QueryBuilder(Type.get(types));
            if (nameSearch) {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Name, input + "*");
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Name);
            } else {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Description, input + "*")
                                                                                                .setIgnoreCase(true);
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Description);
            }
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.ProductAbstract.ID, CIProducts.ProductAbstract.Name,
                            CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.Dimension);
            multi.execute();

            while (multi.next()) {
                final String name = (String) multi.getAttribute(CIProducts.ProductAbstract.Name);
                final String desc = (String) multi.getAttribute(CIProducts.ProductAbstract.Description);
                final Long id =  multi.<Long>getAttribute(CIProducts.ProductAbstract.ID);
                final String choice = nameSearch ? name + " - " + desc : desc + " - " + name;
                final Map<String, String> map = new HashMap<String, String>();
                map.put("eFapsAutoCompleteKEY", id.toString());
                map.put("eFapsAutoCompleteVALUE", name);
                map.put("eFapsAutoCompleteCHOICE", choice);
                map.put("label", name);
                orderMap.put(choice, map);
            }
            list.addAll(orderMap.values());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }


    public Return picker4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Map<String, String> map = new HashMap<String, String>();
        retVal.put(ReturnValues.VALUES, map);
        return retVal;
    }
}
