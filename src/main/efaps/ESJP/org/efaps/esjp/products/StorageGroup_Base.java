/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.products;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("005cbb83-b622-4d75-ba45-fb7bd8d93de3")
@EFapsApplication("eFapsApp-Products")
public abstract class StorageGroup_Base
    extends CommonDocument
{

    /**
     * Method is used for an auto-complete field to get the list of StoragesGroups.
     *
     * @param _parameter parameter as passed from the eFaps API
     * @return Return with Map Auto-complete field
     * @throws EFapsException on error
     */
    public Return autoComplete4StorageGroup(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);

        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, Map<String, String>> tmpMap = new TreeMap<>();
        if (input.length() > 0) {
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
            final boolean nameSearch = Character.isDigit(input.charAt(0));
            if (nameSearch) {
                queryBldr.addWhereAttrMatchValue(CIProducts.StorageGroupAbstract.Name, input + "*").setIgnoreCase(true);
            } else {
                queryBldr.addWhereAttrMatchValue(CIProducts.StorageGroupAbstract.Description, input + "*")
                                .setIgnoreCase(true);
            }
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.StorageGroupAbstract.Name, CIProducts.StorageGroupAbstract.Description);
            multi.execute();
            while (multi.next()) {
                final String name = multi.<String>getAttribute(CIProducts.StorageGroupAbstract.Name);
                final String description = multi.<String>getAttribute(CIProducts.StorageGroupAbstract.Description);
                final Map<String, String> map = new HashMap<>();
                map.put("eFapsAutoCompleteKEY", multi.getCurrentInstance().getOid());
                map.put("eFapsAutoCompleteVALUE", name);
                map.put("eFapsAutoCompleteCHOICE", name + " - " + description);
                tmpMap.put(name, map);
            }
            list.addAll(tmpMap.values());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public List<Instance> getStorage4Group(final Parameter _parameter,
                                           final Instance _storageGroupInst)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink,
                        _storageGroupInst);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageAbstract);
        queryBldr.addWhereAttrInQuery(CIProducts.StorageAbstract.ID, attrQueryBldr.getAttributeQuery(
                        CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink));

        return queryBldr.getQuery().execute();
    }
}
