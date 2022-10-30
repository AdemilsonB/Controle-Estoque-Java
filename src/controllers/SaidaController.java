package controllers;

import java.util.ArrayList;

import models.Produto;
import models.Saida;

public class SaidaController {
	
	private static ArrayList<Saida> saidas  = new ArrayList<Saida>();
    private static double valorProduto;
    private static int quantidade;
    private static double valorTotal;
    private static Produto produto;
    private static int quantidadeEstoque;

      
   //cadastrar
   public static boolean registrar(Saida saida) {
                    
       saidas.add(saida);

       quantidade = saida.getProduto().getQntdEstoque();
       quantidade -= saida.getQntdProduto();
       saida.getProduto().setQntdEstoque(quantidade);

       return true;
   }
   
   //Listar
   
    public static ArrayList<Saida> listar(){
       return saidas;
   }
    
    //Buscar por Id
    
    public static Saida buscarPorId(int id){
       for(Saida saidaCadastrada: saidas){
           if ((saidaCadastrada.getId()) == (id)){
               return saidaCadastrada;
           }
       }

       return null;    
   }
       
    
    //deletar Entrada
       public static Boolean deletar(Saida saida){ 
         
               quantidade = saida.getQntdProduto();
               produto = saida.getProduto();
               quantidadeEstoque = (produto.getQntdEstoque()) + (quantidade);
               
               produto.setQntdEstoque(quantidadeEstoque);
               saidas.remove(saida);
               
               return true;
           }
       
   
   //funções extras
   
   public static double valorTotal(Saida saidaCadastrada){
           valorProduto = saidaCadastrada.getValorProduto();
           quantidade = saidaCadastrada.getQntdProduto();
           valorTotal = valorProduto * quantidade;
           return valorTotal;
   
}
   
public static int quantidadeSaidas(Saida saidaCadastrada){
           return saidaCadastrada.getQntdProduto();
       }
}
