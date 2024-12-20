package com.fiec.ckplanches.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fiec.ckplanches.DTO.ProductCreateDTO;
import com.fiec.ckplanches.DTO.ProductTableDTO;
import com.fiec.ckplanches.DTO.SupplyTableDTO;
import com.fiec.ckplanches.model.enums.Status;
import com.fiec.ckplanches.model.product.Product;
import com.fiec.ckplanches.model.productSupply.ProductSupply;
import com.fiec.ckplanches.model.supply.Supply;
import com.fiec.ckplanches.repositories.ProductRepository;
import com.fiec.ckplanches.repositories.ProductSupplyRepository;
import com.fiec.ckplanches.repositories.SupplyRepository;

@Service
public class ProductService {

    private final Path pastaImagens = Paths.get(System.getProperty("user.home"), "TCCckpLanches", "src", "main", "resources", "ProductImages");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSupplyRepository productSupplyRepository;

    @Autowired
    private SupplyRepository supplyRepository;

    @Autowired
    private SupplyService supplyService;

     public String salvarImagem(MultipartFile file) throws IOException {
        String nomeImagem = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path caminhoImagem = pastaImagens.resolve(nomeImagem);
        Files.copy(file.getInputStream(), caminhoImagem, StandardCopyOption.REPLACE_EXISTING);
        return nomeImagem;  // Retorna o nome ou caminho da imagem
    }

    public String atualizarImagem(int produtoId, MultipartFile novaImagem) throws IOException {
        Product produto = productRepository.findById(produtoId).orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        
        // Caminho da imagem existente
        String caminhoAntigo = pastaImagens + produto.getImagemUrl();
        
        // Verifica se a imagem já existe e apaga o arquivo antigo
        Path caminhoImagemAntiga = Paths.get(caminhoAntigo);
        if (Files.exists(caminhoImagemAntiga)) {
            Files.delete(caminhoImagemAntiga);
        }
        
        // Salva a nova imagem no mesmo local
        String nomeNovaImagem = produto.getImagemUrl();  // Mantém o mesmo nome de arquivo
        Path caminhoNovo = pastaImagens.resolve(nomeNovaImagem);
        Files.copy(novaImagem.getInputStream(), caminhoNovo, StandardCopyOption.REPLACE_EXISTING);
        
        return nomeNovaImagem;  // Retorna o nome da nova imagem (que é o mesmo)
    }

    public ProductTableDTO criarProduto(ProductCreateDTO produtoDTO, MultipartFile imagem) throws IOException {
        
        Product productOptional =  productRepository.findByProductName(produtoDTO.productName());

        if(productOptional != null) throw new IllegalArgumentException("Esse produto já existe!"); 

        // Cria o novo produto
        Product produtoNovo = new Product();
        produtoNovo.setProductName(produtoDTO.productName());
        produtoNovo.setProduct_value(produtoDTO.productValue());
        produtoNovo.setDescription(produtoDTO.description());
        produtoNovo.setCategory(produtoDTO.category());
        produtoNovo.setStatus(Status.ATIVO);

        produtoNovo = productRepository.save(produtoNovo);
        
        //Vincular os insumos ao produto
        if(produtoDTO.supplieNames() != null)
        for(String name:produtoDTO.supplieNames()){
            Supply supplyOptional = supplyRepository.findByName(name);
            if(supplyOptional != null)
            criarProductSupply(produtoNovo, supplyOptional);
        }

        // Salva a imagem
        String nomeImagem = salvarImagem(imagem);
        produtoNovo.setImagemUrl(nomeImagem); // Define a URL da imagem

        // Salva o produto no banco de dados
        return convertProductToTableDTO(productRepository.save(produtoNovo));
}

    public void deleteProductById(Integer productId) throws IOException{
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

            // Caminho da imagem existente
            String caminhoAntigo = pastaImagens + product.getImagemUrl();
            
            // Verifica se a imagem já existe e apaga o arquivo antigo
            Path caminhoImagemAntiga = Paths.get(caminhoAntigo);
            if (Files.exists(caminhoImagemAntiga)) {
                Files.delete(caminhoImagemAntiga);
            }
            
            // Remove todas as productSupplies associadas
            List<ProductSupply> productSupplies = product.getProductSupplies();
            for (ProductSupply supply : productSupplies) {
                productSupplyRepository.delete(supply);
            }

            // Após remover os supplies, exclui o produto
            productRepository.delete(product);
        }

        public void criarProductSupply(Product product, Supply supply){
            ProductSupply productSupply = new ProductSupply();
            productSupply.setProduct(product);
            productSupply.setSupply(supply);
            productSupplyRepository.save(productSupply); 
        }

    

        // Utils

        public ProductTableDTO convertProductToTableDTO(Product product){

            List<SupplyTableDTO> supplyTableDTOs = new ArrayList<>();
            System.out.println("entrei");

            if(product.getProductSupplies() != null)
                for(ProductSupply productSupply:product.getProductSupplies()){
                    Supply supply = productSupply.getSupply();
                    supplyTableDTOs.add(supplyService.convertSupplyToTableDTO(supply));
                }

            return new ProductTableDTO(product.getProduct_id(), 
            product.getProductName(), 
            product.getProduct_value(), 
            product.getImagemUrl(), 
            product.getDescription(),
            product.getCategory(),
            supplyTableDTOs);
        }
}