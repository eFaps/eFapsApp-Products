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

@JsonDeserialize(builder = ProductFamilyResponseDto.Builder.class)
@EFapsUUID("4c27374d-dfcc-45e8-932a-7ea2f298a5c7")
@EFapsApplication("eFapsApp-Products")
public class ProductFamilyResponseDto
{

    private final String current;
    private final List<ProductFamilyDto> families;

    private ProductFamilyResponseDto(Builder builder)
    {
        this.current = builder.current;
        this.families = builder.families;
    }

    public String getCurrent()
    {
        return current;
    }

    public List<ProductFamilyDto> getFamilies()
    {
        return families;
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

        private String current;
        private List<ProductFamilyDto> families = Collections.emptyList();

        private Builder()
        {
        }

        public Builder withCurrent(String current)
        {
            this.current = current;
            return this;
        }

        public Builder withFamilies(List<ProductFamilyDto> families)
        {
            this.families = families;
            return this;
        }

        public ProductFamilyResponseDto build()
        {
            return new ProductFamilyResponseDto(this);
        }
    }
}
