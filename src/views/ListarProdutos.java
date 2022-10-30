package views;

import controllers.ProdutoController;
import models.Produto;

public class ListarProdutos {

	public static void renderizar(){

		System.out.println("\n-- Lista de produtos --");

		for(Produto produtoCadastrado: ProdutoController.listar()){
			System.out.println("\n");    
			System.out.println("Criado em: "+produtoCadastrado.getCriadoEm());
			System.out.println("Nome do produto: "+produtoCadastrado.getNomeProduto());
			System.out.println("Categoria: "+produtoCadastrado.getCategoriaProduto());
			System.out.println("Código do produto: "+produtoCadastrado.getCodigoProduto());
			System.out.println("Quantidade em estoque: "+produtoCadastrado.getQntdEstoque());
		}
	}
}


