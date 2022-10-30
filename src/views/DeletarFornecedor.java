package views;

import java.util.Scanner;

import controllers.FornecedorController;
import models.Fornecedor;

public class DeletarFornecedor {

	private static Scanner print = new Scanner(System.in);
	private static Fornecedor fornecedor;

	public static void renderizar(){
		
		fornecedor = new Fornecedor();
		   
		System.out.println("\nDigite o CNPJ do fornecedor que deseja deletar");
		String cnpj = print.next();
		fornecedor = FornecedorController.buscarPorCnpj(cnpj);
		if (fornecedor != null){
			if(FornecedorController.deletar(cnpj)){
				System.out.println("\nFornecedor excluído com sucesso");
			}
		} else{
			System.out.println("\nEste fornecedor não existe");
		}
	}
}
