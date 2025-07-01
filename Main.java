import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main{ static DefaultListModel<String> listModel = new DefaultListModel<>(); static JList<String> bookList = new JList<>(listModel); static java.util.List<String> cart = new ArrayList<>(); static Map<String, String> imagePaths = new HashMap<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame("📚 Kitob Do'koni");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700);
        frame.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField priceField = new JTextField();
        JComboBox<String> genreBox = new JComboBox<>(new String[]{"Badiiy", "Ilmiy", "Detektiv", "Tarixiy"});
        JButton imageButton = new JButton("🖼 Rasm tanlash");
        JLabel imagePathLabel = new JLabel("");
        JButton addButton = new JButton("➕ Qo‘shish");
        JButton editButton = new JButton("✏️ Tahrirlash");
        JButton deleteButton = new JButton("🗑 O‘chirish");
        JButton cartButton = new JButton("🛒 Savat");
        JButton clearAllButton = new JButton("❌ Barchasini o‘chirish");
        JButton exportCSV = new JButton("📄 CSVga eksport");
        JTextField searchField = new JTextField();

        formPanel.add(new JLabel("🔍 Qidiruv:"));
        formPanel.add(searchField);
        formPanel.add(new JLabel("Kitob nomi:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel("Muallif:"));
        formPanel.add(authorField);
        formPanel.add(new JLabel("Narxi ($):"));
        formPanel.add(priceField);
        formPanel.add(new JLabel("Janri:"));
        formPanel.add(genreBox);
        formPanel.add(imageButton);
        formPanel.add(imagePathLabel);
        formPanel.add(addButton);
        formPanel.add(editButton);
        formPanel.add(deleteButton);
        formPanel.add(cartButton);

        JScrollPane scrollPane = new JScrollPane(bookList);
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(clearAllButton);
        bottomPanel.add(exportCSV);

        frame.add(formPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        loadBooksFromFile();
        loadImagesFromFile();

        imageButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Rasm fayllar", "jpg", "png", "jpeg"));
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                imagePathLabel.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        addButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String price = priceField.getText().trim();
            String genre = (String) genreBox.getSelectedItem();
            String imagePath = imagePathLabel.getText();

            if (title.isEmpty() || author.isEmpty() || price.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Iltimos, barcha maydonlarni to‘ldiring.");
                return;
            }
            try {
                Double.parseDouble(price);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Narx faqat son bo'lishi kerak.");
                return;
            }
            String entry = title + " - " + author + " - $" + price + " - " + genre;
            listModel.addElement(entry);
            if (!imagePath.isEmpty()) imagePaths.put(entry, imagePath);
            saveAllBooksToFile();
            saveImagesToFile();
            titleField.setText(""); authorField.setText(""); priceField.setText(""); imagePathLabel.setText("");
        });
        editButton.addActionListener(e -> {
            int idx = bookList.getSelectedIndex();
            if (idx != -1) {
                String selected = listModel.get(idx);
                String[] parts = selected.split(" - ");
                titleField.setText(parts[0]);
                authorField.setText(parts[1]);
                priceField.setText(parts[2].replace("$", ""));
                genreBox.setSelectedItem(parts[3]);
                imagePathLabel.setText(imagePaths.getOrDefault(selected, ""));
                listModel.remove(idx);
            }
        });

        deleteButton.addActionListener(e -> {
            int idx = bookList.getSelectedIndex();
            if (idx != -1) {
                String selected = listModel.get(idx);
                listModel.remove(idx);
                imagePaths.remove(selected);
                saveAllBooksToFile();
                saveImagesToFile();
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String query = searchField.getText().toLowerCase();
                listModel.clear();
                try (BufferedReader reader = new BufferedReader(new FileReader("books.txt"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains(query)) listModel.addElement(line);
                    }
                } catch (IOException ignored) {}
            }
        });

        clearAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame, "Barchasini o‘chirishga ishonchingiz komilmi?", "Tasdiqlash", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                listModel.clear();
                imagePaths.clear();
                saveAllBooksToFile();
                saveImagesToFile();
            }
        });

        exportCSV.addActionListener(e -> {
            try (FileWriter writer = new FileWriter("books.csv")) {
                for (int i = 0; i < listModel.size(); i++) {
                    writer.write(listModel.get(i).replace(" - ", ",") + "\n");
                }
                JOptionPane.showMessageDialog(frame, "CSV fayl yaratildi: books.csv");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Xatolik yuz berdi!");
            }
        });

        cartButton.addActionListener(e -> openCartWindow());
        frame.setVisible(true);
    }

    private static void openCartWindow() {
        JFrame cartFrame = new JFrame("🛒 Savatcha");
        cartFrame.setSize(400, 400);
        cartFrame.setLayout(new BorderLayout());

        DefaultListModel<String> cartModel = new DefaultListModel<>();
        JList<String> cartList = new JList<>(cartModel);
        JScrollPane scroll = new JScrollPane(cartList);
        JButton addToCart = new JButton("➕ Savatga qo‘shish");
        JButton checkout = new JButton("💳 Sotib olish");
        JLabel totalLabel = new JLabel("💰 Umumiy: $0.00");

        JPanel panel = new JPanel(new GridLayout(2, 1));
        JPanel btnPanel = new JPanel();
        btnPanel.add(addToCart);
        btnPanel.add(checkout);
        panel.add(btnPanel);
        panel.add(totalLabel);

        cartFrame.add(scroll, BorderLayout.CENTER);
        cartFrame.add(panel, BorderLayout.SOUTH);

        addToCart.addActionListener(e -> {
            String selected = bookList.getSelectedValue();
            if (selected != null && !cart.contains(selected)) {
                cart.add(selected);
                cartModel.addElement(selected);
                updateTotal(cartModel, totalLabel);
            }
        });

        checkout.addActionListener(e -> {
            String card = JOptionPane.showInputDialog(cartFrame, "💳 Kartangiz raqamini kiriting:");
            if (card != null && card.matches("\\d{16}")) {
                try (FileWriter writer = new FileWriter("orders.txt", true)) {
                    for (String item : cart) {
                        writer.write("Sotib olindi: " + item + " | Karta: **** " + card.substring(12) + "\n");
                    }
                    JOptionPane.showMessageDialog(cartFrame, "✅ Buyurtma muvaffaqiyatli! Rahmat.");
                    cart.clear();
                    cartModel.clear();
                    updateTotal(cartModel, totalLabel);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(cartFrame, "❌ Karta raqami noto‘g‘ri.");
            }
        });

        cartFrame.setVisible(true);
    }

    private static void updateTotal(DefaultListModel<String> model, JLabel label) {
        double sum = 0;
        for (int i = 0; i < model.size(); i++) {
            try {
                String[] parts = model.get(i).split(" - ");
                sum += Double.parseDouble(parts[2].replace("$", ""));
            } catch (Exception ignored) {}
        }
        label.setText("💰 Umumiy: $" + String.format("%.2f", sum));
    }

    private static void loadBooksFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("books.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) listModel.addElement(line);
        } catch (IOException ignored) {}
    }

    private static void saveAllBooksToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("books.txt"))) {
            for (int i = 0; i < listModel.size(); i++) writer.write(listModel.get(i) + "\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void saveImagesToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("images.txt"))) {
            for (Map.Entry<String, String> entry : imagePaths.entrySet()) {
                writer.write(entry.getKey() + "||" + entry.getValue() + "\n");
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadImagesFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("images.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|\\|");
                if (parts.length == 2) imagePaths.put(parts[0], parts[1]);
            }
        } catch (IOException ignored) {}
    }
}