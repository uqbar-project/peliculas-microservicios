package org.uqbar.peliculasmicroserviceranking.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.uqbar.peliculasmicroserviceranking.domain.Pelicula
import org.uqbar.peliculasmicroserviceranking.repository.PeliculaRepository
import reactor.core.publisher.Mono

@Service
@Transactional
class PeliculaService {

   @Autowired
   lateinit var peliculaRepository: PeliculaRepository

   @Autowired
   lateinit var tmdbService: TMDBService

   val logger = LoggerFactory.getLogger(PeliculaService::class.java)

   suspend fun buscarPelicula(_idTMDB: Int): Mono<Pelicula?> {
      logger.info("Buscando película con id TMDB = ${_idTMDB}")
      return peliculaRepository.findByIdTMDB(_idTMDB).switchIfEmpty(
         // En caso de que no encontremos la película en nuestra base mongo traemos la información de TMDB
         // y guardamos la información. El método save de un ReactiveRepository devuelve un Mono<Pelicula>
         // esto significa que es un Future (una promesa de una película grabada como documento en la base)
         Mono.defer {
            val pelicula = tmdbService.buscarPeliculaPorId(_idTMDB)
            peliculaRepository.save(pelicula)
         })
   }

   suspend fun verPelicula(idTMDB: Int): Mono<Pelicula> {
      logger.info("Visualizar película ${idTMDB}")
      return buscarPelicula(idTMDB).map { pelicula ->
         pelicula!!.sumarVista()
         pelicula
      }.doOnNext { pelicula ->
         logger.info("Película tiene ${pelicula.vistas} vistas")
         // La llamada al subscribe es muy importante para que se dispare la actualización
         peliculaRepository.save(pelicula).subscribe()
      }
   }
}