package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


@ExtendWith(WireMockExtension.class)
public class MoviesRestClientTest {

    MoviesRestClient moviesRestClient;
    WebClient webClient;

    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig().port(8088).
            notifier(new ConsoleNotifier(true)).
            extensions(new ResponseTemplateTransformer(true));

    @BeforeEach
    void setUp(){

        int port = wireMockServer.port();

        String baseURL = String.format("http://localhost:%s", port);
        System.out.println("baseUrl: " + baseURL);

        webClient = WebClient.create(baseURL);
        moviesRestClient = new MoviesRestClient(webClient);

    }

    @Test
    void retrieveAllMovies(){
//        given
        stubFor(get(anyUrl()).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("all-Movies.json")));

//        when
        List<Movie> movieList = moviesRestClient.retrieveAllMovies();
//        System.out.println("movie list: " + movieList);
//        then
        Assertions.assertTrue(movieList.size()>0);
    }

    @Test
    void retrieveAllMovies_matchesUrl(){
//        given
        stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1)).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("all-Movies.json")));

//        when
        List<Movie> movieList = moviesRestClient.retrieveAllMovies();
//        System.out.println("movie list: " + movieList);
//        then
        Assertions.assertTrue(movieList.size()>0);
    }

    @Test
    void retrieveMovieById(){
//        given
        Integer movieId = 1;
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]")).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("movie.json")));

//        when
        Movie movie = moviesRestClient.retrieveMovieById(movieId);
        Assertions.assertEquals("Batman Begins", movie.getName());
    }

    @Test
    void retrieveMovieById_responseTemplating(){
//        given
        Integer movieId = 8;
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]")).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("movie-template.json")));

//        when
        Movie movie = moviesRestClient.retrieveMovieById(movieId);
        System.out.println("movie:" + movie);

        Assertions.assertEquals("Batman Begins", movie.getName());
        Assertions.assertEquals(8, movie.getMovie_id().intValue());
    }

    @Test
    void retrieveMovieById_notFound(){
//        given
        Integer movieId = 100;
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+")).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.NOT_FOUND.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("404-movieid.json")));

//        when
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieById(movieId));
    }

    @Test
    void retrieveMovieByName(){

//        given
        String name = "Avengers";
        stubFor(get(urlEqualTo(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name="+name)).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("avengers.json")));
//        when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(name);
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
//        then
        Assertions.assertEquals(4, movieList.size());
        Assertions.assertEquals(castExpected, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByName_approach2(){

//        given
        String name = "Avengers";
        stubFor(get(urlPathEqualTo(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1)).
                        withQueryParam("movie_name", equalTo(name)).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("avengers.json")));
//        when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(name);
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
//        then
        Assertions.assertEquals(4, movieList.size());
        Assertions.assertEquals(castExpected, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByName_responseTemplating(){

//        given
        String name = "Avengers";
        stubFor(get(urlEqualTo(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name="+name)).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("movie-byName-template.json")));
//        when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(name);
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
//        then
        Assertions.assertEquals(4, movieList.size());
        Assertions.assertEquals(castExpected, movieList.get(0).getCast());
    }


    @Test
    void retrieveMovieByName_NotFound(){

//        given
        String name = "ABC";

//        when

//        then
        Assertions.assertThrows(MovieErrorResponse.class, () ->  moviesRestClient.retrieveMovieByName(name));
    }


    @Test
    void retrieveMovieByYear(){

//        given
        Integer year = 2005;
        stubFor(get(urlEqualTo(MoviesAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1+"?year="+year)).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("movie.json")));

//        when
        List<Movie> movieList = moviesRestClient.retrieveMovieByYear(year);

//        then
        Assertions.assertEquals(1, movieList.size());
    }

    @Test
    void retrieveMovieByYear_responseTemplating(){

//        given
        Integer year = 2012;
        stubFor(get(urlEqualTo(MoviesAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1+"?year="+year)).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("year-template.json")));

//        when
        List<Movie> movieList = moviesRestClient.retrieveMovieByYear(year);

//        then
        Assertions.assertEquals(2, movieList.size());
    }

    @Test
    void retrieveMovieByYear_notFound(){

//        given
        Integer year = 1950;
        stubFor(get(urlEqualTo(MoviesAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1+"?year="+year)).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.NOT_FOUND.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("404-movieyear.json")));
//        when
//        then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByYear(year));
    }

    @Test
    void addMovie(){
        //        given
       Movie movie = new Movie(null, "Toys Story 4", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1)).
                withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 4"))).
                withRequestBody(matchingJsonPath(("$.cast"), containing("Tom Hanks"))).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("add-movie.json")));

//        when
        Movie movie1 = moviesRestClient.addMovie(movie);

//        then
        Assertions.assertTrue(movie1.getMovie_id() != null);

    }

    @Test
    void addMovie_templating(){
        //        given
        Movie movie = new Movie(null, "Toys Story 4", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1)).
                withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 4"))).
                withRequestBody(matchingJsonPath(("$.cast"), containing("Tom Hanks"))).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("add-movie-template.json")));

//        when
        Movie movie1 = moviesRestClient.addMovie(movie);

//        then
        Assertions.assertTrue(movie1.getMovie_id() != null);

    }

    @Test
    void addMovie_badRequest(){
        //        given
        Movie movie = new Movie(null, null, "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1)).
                withRequestBody(matchingJsonPath(("$.cast"), containing("Tom Hanks"))).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.BAD_REQUEST.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("404-invalid-input.json")));

//        when
        String expectedErrorMessage = "Please pass all the input fields : [name]";

//        then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.addMovie(movie));
    }

    @Test
    void updateMovie(){
//        given
        Integer movieId = 3;
        String castMember = "ABC";
        Movie movie = new Movie(null, null, castMember, null, null);
        stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+")).
                withRequestBody(matchingJsonPath(("$.cast"), containing(castMember))).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("update-movie-template.json")));


//        when
        Movie updatedMovie = moviesRestClient.updateMovie(movieId, movie);

//        then
        Assertions.assertTrue(updatedMovie.getCast().contains(castMember));
    }

    @Test
    void updateMovie_notFound(){
//        given
        Integer movieId = 100;
        String castMember = "ABC";
        Movie movie = new Movie(null, null, castMember, null, null);

        stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+")).
                withRequestBody(matchingJsonPath(("$.cast"), containing(castMember))).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.NOT_FOUND.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

//        when
//        then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movieId, movie));
    }

    @Test
    void deleteMovie(){
//        given

        Movie movie = new Movie(null, "Toys Story 4", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1)).
                withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 4"))).
                withRequestBody(matchingJsonPath(("$.cast"), containing("Tom Hanks"))).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("add-movie-template.json")));

        Movie movie1 = moviesRestClient.addMovie(movie);

//        when
        String expectedMessage = "Movie Deleted Successfully";
        stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+")).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBody(expectedMessage)));
        String responseMessage =  moviesRestClient.deleteMovie(movie1.getMovie_id().intValue());

//        then

        Assertions.assertEquals(expectedMessage, responseMessage);

    }
    @Test
    void deleteMovie_notFound(){
//        given

        Integer movieId = 100;
        stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+")).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.NOT_FOUND.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
//        when
//        then
        String expectedMessage = "Movie Deleted Successfully";
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(movieId));

    }

    @Test
    void deleteMovieByName(){
//        given

        Movie movie = new Movie(null, "Toys Story 4", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1)).
                withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 4"))).
                withRequestBody(matchingJsonPath(("$.cast"), containing("Tom Hanks"))).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBodyFile("add-movie-template.json")));

        Movie movie1 = moviesRestClient.addMovie(movie);

//        when
        String expectedMessage = "Movie Deleted Successfully";
        stubFor(delete(urlEqualTo(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name=Toys%20Story%204")).
                willReturn(WireMock.aResponse().
                        withStatus(HttpStatus.OK.value()).
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBody(expectedMessage)));
        String responseMessage =  moviesRestClient.deleteMovieByName(movie1.getName());

//        then

        Assertions.assertEquals(expectedMessage, responseMessage);

        verify(exactly(1), postRequestedFor(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1)).
                withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 4"))).
                withRequestBody(matchingJsonPath(("$.cast"), containing("Tom Hanks"))));
        verify(exactly(1), deleteRequestedFor(urlEqualTo
                (MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name=Toys%20Story%204")));

    }

}