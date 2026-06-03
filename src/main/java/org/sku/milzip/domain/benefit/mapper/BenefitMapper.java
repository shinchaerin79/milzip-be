package org.sku.milzip.domain.benefit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.sku.milzip.domain.benefit.dto.response.AmusementParkBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.MovieBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.SelfDevelopmentBenefitResponse;
import org.sku.milzip.domain.benefit.entity.Benefit;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BenefitMapper {

  MovieBenefitResponse toMovieResponse(Benefit benefit);

  AmusementParkBenefitResponse toAmusementParkResponse(Benefit benefit);

  SelfDevelopmentBenefitResponse toSelfDevelopmentResponse(Benefit benefit);
}
