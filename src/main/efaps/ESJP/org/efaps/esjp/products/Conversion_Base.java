/*
 * Copyright 2003 - 2020 The eFaps Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.products.util.Products;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;

@EFapsUUID("c635be92-213a-4f5c-89fe-d5b37b11bc85")
@EFapsApplication("eFapsApp-Products")
public abstract class Conversion_Base
{

    public Return fromUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final var instance = (Instance) _parameter.get(ParameterValues.CALL_INSTANCE);
        final List<DropDownPosition> values = new ArrayList<>();
        if (InstanceUtils.isKindOf(instance, CIProducts.ProductAbstract)) {
            final var eval = EQL.builder().print(instance).attribute(CIProducts.ProductAbstract.DefaultUoM).evaluate();
            final Long defaultUoM = eval.get(CIProducts.ProductAbstract.DefaultUoM);
            if (defaultUoM != null) {
                final Dimension dim = Dimension.getUoM(defaultUoM).getDimension();
                for (final UoM uoM : dim.getUoMs()) {
                    values.add(new DropDownPosition(uoM.getId(), uoM.getName())
                                    .setSelected(dim.getBaseUoM() != null && dim.getBaseUoM().getId() == defaultUoM));
                }
            }
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    public Return toUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = (Instance) _parameter.get(ParameterValues.CALL_INSTANCE);
        Properties props;
        if (InstanceUtils.isType(instance, CIProducts.ProductStandart)) {
            props = Products.STANDART_CONV.get();
        } else {
            props = new Properties();
        }
        final List<DropDownPosition> values = new ArrayList<>();
        if (instance != null) {
            for (final var dimKey : PropertiesUtil.analyseProperty(props, "Dimension", 0).values()) {
                final Dimension dim = UUIDUtil.isUUID(dimKey) ? Dimension.get(UUID.fromString(dimKey))
                                : Dimension.get(dimKey);
                if (dim != null) {
                    for (final UoM uoM : dim.getUoMs()) {
                        values.add(new DropDownPosition(uoM.getId(), dim.getName() + " - " + uoM.getName()));
                    }
                }
            }

        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }
}
