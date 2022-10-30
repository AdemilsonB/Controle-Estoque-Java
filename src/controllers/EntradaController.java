package controllers;

import java.util.ArrayList;

import models.Entrada;
import models.Produto;

public class EntradaController {

	private static ArrayList<Entrada> entradas = new ArrayList<Entrada>();
	private static  int quantidade;
	private static Produto produto;
	private static int quantidadeEstoque;
	private static double custoUnidade;
	private static double valorGasto;



	//Cadastrar
	public static Boolean registrar(Entrada entrada){

		entradas.add(entrada);

		quantidade = entrada.getProduto().getQntdEstoque();

		quantidade += entrada.getQuantidade();
		entrada.getProduto().setQntdEstoque(quantidade);

		return true;

	}

	public static double valorGasto(Entrada entradaCadastrada){
		custoUnidade = entradaCadastrada.getCustoUnidade();
		quantidade = entradaCadastrada.getQuantidade();
		valorGasto = custoUnidade * quantidade;
		return valorGasto;
	}

	public static int quantidadeEntradas(Entrada entradaCadastrada){
		return entradaCadastrada.getQuantidade();
	}

	//Listar todas as entradas
	public static ArrayList<Entrada> listar(){
		return entradas;
	}

	//Buscar por Produto

	public static Entrada buscarPorProduto(String nomeProduto){
		for(Entrada entradaCadastrada: entradas){
			if (entradaCadastrada.getProduto().getNomeProduto().equals(nomeProduto)){
				return entradaCadastrada;
			}
		}

		return null;    
	}

	//Buscar por ID

	public static Entrada buscarPorId(int id){
		for(Entrada entradaCadastrada: entradas){
			if ((entradaCadastrada.getId()) == (id)){
				return entradaCadastrada;
			}
		}

		return null;    
	}


	//deletar Entrada
	public static Boolean deletar(Entrada entrada){ 

		quantidade = entrada.getQuantidade();
		produto = entrada.getProduto();
		quantidadeEstoque = (produto.getQntdEstoque()) - (quantidade);

		produto.setQntdEstoque(quantidadeEstoque);
		entradas.remove(entrada);

		return true;
	}
}
