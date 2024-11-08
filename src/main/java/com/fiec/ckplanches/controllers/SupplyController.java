package com.fiec.ckplanches.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fiec.ckplanches.DTO.SupplyDTO;
import com.fiec.ckplanches.DTO.SupplyTableDTO;
import com.fiec.ckplanches.DTO.SupplyUpdateDTO;
import com.fiec.ckplanches.model.enums.TypeMovement;
import com.fiec.ckplanches.model.movement.Movement;
import com.fiec.ckplanches.model.supply.Supply;
import com.fiec.ckplanches.repositories.LotRepository;
import com.fiec.ckplanches.repositories.MovementRepository;
import com.fiec.ckplanches.repositories.SupplyRepository;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/insumos")
public class SupplyController {

    @Autowired
    private SupplyRepository dao;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LogController logController;

    @Autowired
    private MovementRepository movementRepository;

    @GetMapping
    @Secured("ADMIN")
    public List<SupplyTableDTO> listarInsumos() {
        List<Supply> supplies = dao.findAll();
        List<SupplyTableDTO> supplyDTOs = new ArrayList<>(); // Inicialize a lista

        for (Supply element : supplies) {
            SupplyTableDTO supplyDTO = new SupplyTableDTO(
                element.getId(),
                element.getName(),
                element.getDescription(),
                element.getQuantity(),
                element.getMinQuantity(),
                element.getMaxQuantity()
            );
            supplyDTOs.add(supplyDTO);
        }
        
        return supplyDTOs;
    }


    @PostMapping
    @Secured("ADMIN")
    public ResponseEntity<?> criarInsumo(@RequestBody SupplyDTO insumo, @AuthenticationPrincipal UserDetails userDetails) {
        try{
            Supply insumoNovo = new Supply();
            insumoNovo.setName(insumo.name());
            insumoNovo.setDescription(insumo.description());
            insumoNovo.setMinQuantity(insumo.minQuantity());
            insumoNovo.setMaxQuantity(insumo.maxQuantity());
            insumoNovo.setQuantity(insumo.quantity());
            insumoNovo = dao.save(insumoNovo);
            if(insumoNovo.getQuantity() > 0) {
                Movement movement = new Movement(LocalDateTime.now(), insumoNovo.getQuantity(), TypeMovement.ENTRADA, insumoNovo);
                movementRepository.save(movement);
            }
            logController.logAction(userDetails.getUsername(), "Criou um insumo", insumoNovo.getId());
            return ResponseEntity.ok(Map.of("result", new SupplyTableDTO(
            insumoNovo.getId(),
            insumoNovo.getName(),
            insumoNovo.getDescription(),
            insumoNovo.getQuantity(),
            insumoNovo.getMinQuantity(),
            insumoNovo.getMaxQuantity())
            ));
        }
        catch(Exception erro){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro inesperado no servidor");
        }
    }

    @PutMapping("/{id}")
    @Secured("ADMIN")
    public ResponseEntity<?> editarInsumo(@RequestBody SupplyUpdateDTO insumo, @PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails) {
        try{
            System.out.println(insumo);
            Supply insumoNovo = null;
            Optional<Supply> novoInsumo = dao.findById(id);
            if(novoInsumo.isPresent()){
                insumoNovo = novoInsumo.get();
                insumoNovo.setName(insumo.name());
                insumoNovo.setDescription(insumo.description());
                insumoNovo.setMinQuantity(insumo.minQuantity());
                insumoNovo.setMaxQuantity(insumo.maxQuantity());
                insumoNovo = dao.save(insumoNovo);
                logController.logAction(userDetails.getUsername(), "Atualizou um insumo", insumoNovo.getId());
            }
            else{
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Este Insumo não existe");
            }
            
            return ResponseEntity.ok(Map.of("result", new SupplyTableDTO(insumoNovo.getId(), 
            insumoNovo.getName(), 
            insumoNovo.getDescription(), 
            insumoNovo.getQuantity(), 
            insumoNovo.getMinQuantity(), 
            insumoNovo.getMaxQuantity())
            ));
        }
        catch(Exception erro){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro inesperado no servidor");
        }
    }

    @DeleteMapping("/{id}")
    @Secured("ADMIN")
    public void deletarInsumo(@PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails) {
        if (dao.existsById(id)) {
            dao.deleteById(id);
            logController.logAction(userDetails.getUsername(), "Deletou um insumo", id);
        } else {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo não encontrado");
        }
    }

    @GetMapping("/{name}")
    @Secured("ADMIN")
    public Supply findByName(@PathVariable String nome) {
       return dao.findByName(nome) ;
    }

    @GetMapping("/procurar/{id}")
    @Secured("ADMIN")
    public Supply findById(@PathVariable Integer id) {
        return dao.findById(id) 
        .orElseThrow(() -> new EntityNotFoundException("Insumo com ID " + id + " não encontrado"));
}
        
}