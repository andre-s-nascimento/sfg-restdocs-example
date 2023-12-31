package guru.springframework.sfgrestdocsexample.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.sfgrestdocsexample.domain.Beer;
import guru.springframework.sfgrestdocsexample.repositories.BeerRepository;
import guru.springframework.sfgrestdocsexample.web.model.BeerDto;
import guru.springframework.sfgrestdocsexample.web.model.BeerStyleEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "dev.snascimento", uriPort = 80)
@WebMvcTest(BeerController.class)
@ComponentScan(basePackages = "guru.springframework.sfgrestdocsexample.web.mappers")
class BeerControllerTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockBean BeerRepository beerRepository;

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation)).build();
  }

  @Test
  void getBeerById() throws Exception {
    given(beerRepository.findById(any())).willReturn(Optional.of(Beer.builder().build()));

    mockMvc
        .perform(
            get("/api/v1/beer/{beerId}", UUID.randomUUID().toString())
                .param("iscold", "yes")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "v1/beer-get",
                responseFields(
                    fieldWithPath("id").description("Id of Beer").type(UUID.class),
                    fieldWithPath("version").description("Version number").type(Integer.class),
                    fieldWithPath("createdDate").description("Date Created").type(OffsetDateTime.class),
                    fieldWithPath("lastModifiedDate").description("Date Updated").type(OffsetDateTime.class),
                    fieldWithPath("beerName").description("Beer Name").type(String.class),
                    fieldWithPath("beerStyle").description("Beer Style").type(BeerStyleEnum.class),
                    fieldWithPath("upc").description("UPC of Beer").type(Long.class),
                    fieldWithPath("price").description("Price").type(BigDecimal.class),
                    fieldWithPath("quantityOnHand").description("Quantity On hand").type(Integer.class)),
                queryParameters(
                    parameterWithName("iscold").description("Is Beer Cold Query param.")),
                pathParameters(
                    parameterWithName("beerId").description("UUID of desired beer to get."))));
  }

  @Test
  void saveNewBeer() throws Exception {
    BeerDto beerDto = getValidBeerDto();
    String beerDtoJson = objectMapper.writeValueAsString(beerDto);

    ConstrainedFields fields = new ConstrainedFields(BeerDto.class);

    mockMvc
        .perform(post("/api/v1/beer").contentType(MediaType.APPLICATION_JSON).content(beerDtoJson))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "v1/beer-new",
                requestFields(
                    fields.withPath("id").ignored(),
                    fields.withPath("version").ignored(),
                    fields.withPath("createdDate").ignored(),
                    fields.withPath("lastModifiedDate").ignored(),
                    fields.withPath("beerName").description("Name of the desired beer."),
                    fields.withPath("beerStyle").description("Style of the Beer."),
                    fields.withPath("upc").description("Beer UPC").attributes(),
                    fields.withPath("price").description("Beer Price"),
                    fields.withPath("quantityOnHand").ignored())));
  }

  @Test
  void updateBeerById() throws Exception {
    BeerDto beerDto = getValidBeerDto();
    String beerDtoJson = objectMapper.writeValueAsString(beerDto);

    mockMvc
        .perform(
            put("/api/v1/beer/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
        .andExpect(status().isNoContent());
  }

  BeerDto getValidBeerDto() {
    return BeerDto.builder()
        .beerName("Nice Ale")
        .beerStyle(BeerStyleEnum.ALE)
        .price(new BigDecimal("9.99"))
        .upc(123123123123L)
        .build();
  }

  private static class ConstrainedFields {
    private final ConstraintDescriptions constraintDescriptions;

    public ConstrainedFields(Class<?> input) {
      this.constraintDescriptions = new ConstraintDescriptions(input);
    }

    private FieldDescriptor withPath(String path) {
      return fieldWithPath(path)
          .attributes(
              key("constraints")
                  .value(
                      StringUtils.collectionToDelimitedString(
                          this.constraintDescriptions.descriptionsForProperty(path), ". ")));
    }
  }
}
