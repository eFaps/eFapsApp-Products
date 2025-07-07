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
package org.efaps.esjp.products.rest.modules;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@EFapsUUID("89f7c4e9-1849-4a90-8e2c-b38fa2f323c5")
@EFapsApplication("eFapsApp-Products")
@JsonDeserialize(builder = ProductFamilyDto.Builder.class)
public class ProductFamilyDto
{

    private final String oid;
    private final String label;
    private final String codePart;
    private final String parentOid;
    private final List<ProductFamilyDto> children;

    private ProductFamilyDto(Builder builder)
    {
        this.oid = builder.oid;
        this.label = builder.label;
        this.codePart = builder.codePart;
        this.parentOid = builder.parentOid;
        this.children = builder.children;
    }

    public String getOid()
    {
        return oid;
    }

    public String getLabel()
    {
        return label;
    }

    public String getCodePart()
    {
        return codePart;
    }

    public String getParentOid()
    {
        return parentOid;
    }

    public List<ProductFamilyDto> getChildren()
    {
        return children;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private String oid;
        private String label;
        private String codePart;
        private String parentOid;
        private List<ProductFamilyDto> children = Collections.emptyList();

        private Builder()
        {
        }

        public Builder withOid(String oid)
        {
            this.oid = oid;
            return this;
        }

        public Builder withLabel(String label)
        {
            this.label = label;
            return this;
        }

        public Builder withCodePart(String codePart)
        {
            this.codePart = codePart;
            return this;
        }

        public Builder withParentOid(String parentOid)
        {
            this.parentOid = parentOid;
            return this;
        }

        public Builder withChildren(List<ProductFamilyDto> children)
        {
            this.children = children;
            return this;
        }

        public ProductFamilyDto build()
        {
            return new ProductFamilyDto(this);
        }
    }

}
