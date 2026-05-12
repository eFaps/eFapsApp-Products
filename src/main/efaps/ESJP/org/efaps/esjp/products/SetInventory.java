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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.ui.rest.provider.AbstractTableProvider;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("6f5e4264-8986-4b96-aa02-e710244f1670")
@EFapsApplication("eFapsApp-Products")
public class SetInventory
    extends AbstractTableProvider
{

    private static final Logger LOG = LoggerFactory.getLogger(SetInventory.class);

    @Override
    public Collection<Map<String, ?>> getValues()
        throws EFapsException
    {
        final var inventoryInsts = getSelectedOids().stream().map(Instance::get)
                        .filter(inst -> InstanceUtils.isKindOf(inst, CIProducts.InventoryAbstract))
                        .toArray(Instance[]::new);
        final var eval = EQL.builder().print(inventoryInsts)
                        .attribute(CIProducts.InventoryAbstract.Quantity)
                        .linkto(CIProducts.InventoryAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Name).as("productName")
                        .linkto(CIProducts.InventoryAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Description).as("productDesc")
                        .linkto(CIProducts.InventoryAbstract.UoM).attribute("Name").as("uom")
                        .evaluate();
        final Map<String, Map<String, ?>> objects = new HashMap<>();
        while (eval.next()) {
            final var map = new HashMap<String, Object>();
            map.put("quantityInStock", eval.get(CIProducts.InventoryAbstract.Quantity));
            map.put("alteration", "");
            map.put("productName", eval.get("productName"));
            map.put("productDesc", eval.get("productDesc"));
            map.put("uoM", eval.get("uom"));
            objects.put(eval.inst().getOid(), map);
        }
        final List<Map<String, ?>> values = new ArrayList<>();
        for (final var oid : getSelectedOids()) {
            values.add(objects.containsKey(oid) ? objects.get(oid) : new HashMap<>());
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    public Return updateFields4Quantity(final Parameter parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final var payload = (Map<String, Object>) parameter.get(ParameterValues.PAYLOAD);
        final var selectedOids = (List<String>) payload.get("eFapsSelectedOids");
        final var quantities = (List<String>) payload.get("quantity");
        final DecimalFormat formatter = NumberFormatter.get().getTwoDigitsFormatter();
        final Collection<Map<String, Object>> valueList = new ArrayList<>();
        int idx = 0;
        for (final String oid : selectedOids) {
            final Map<String, Object> map = new HashMap<>();
            valueList.add(map);
            map.put("alteration", "");

            final var quantityStr = quantities.get(idx);
            if (StringUtils.isNotEmpty(quantityStr)) {
                try {
                    final var quantity = (BigDecimal) formatter.parse(quantityStr);
                    final var eval = EQL.builder().print(oid)
                                    .attribute(CIProducts.InventoryAbstract.Quantity)
                                    .evaluate();
                    if (eval.next()) {
                        final BigDecimal quantityInStock = eval.get(CIProducts.InventoryAbstract.Quantity);
                        map.put("alteration", quantity.subtract(quantityInStock));
                    }
                } catch (final ParseException e) {
                    LOG.error("Catched", e);
                }
            }
            idx++;
        }
        ret.put(ReturnValues.VALUES, valueList);
        return ret;
    }

    public Return execute(final Parameter parameter)
        throws EFapsException
    {
        return new Transaction().setInventory(parameter);
    }
}
