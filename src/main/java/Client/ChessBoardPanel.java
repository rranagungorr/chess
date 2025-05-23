package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ChessBoardPanel extends JPanel {

    private JButton[][] squares = new JButton[8][8];
    private ChessClient client;
    private JButton selected = null;
    private int selectedRow, selectedCol;

    private JPanel capturedWhitePanel = new JPanel();
    private JPanel capturedBlackPanel = new JPanel();
    private List<Icon> capturedWhite = new ArrayList<>();
    private List<Icon> capturedBlack = new ArrayList<>();

    public ChessBoardPanel(ChessClient client) {
        this.client = client;
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        setupBoard(boardPanel);

        // Dikey ve sabit genişlikte yenen taş panelleri
        capturedWhitePanel.setLayout(new BoxLayout(capturedWhitePanel, BoxLayout.Y_AXIS));
        capturedWhitePanel.setPreferredSize(new Dimension(70, 480));

        capturedBlackPanel.setLayout(new BoxLayout(capturedBlackPanel, BoxLayout.Y_AXIS));
        capturedBlackPanel.setPreferredSize(new Dimension(70, 480));

        JButton resignButton = new JButton("Oyunu Bırak");
        resignButton.addActionListener(e -> handleResign());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(resignButton, BorderLayout.CENTER);

        add(capturedWhitePanel, BorderLayout.WEST);
        add(boardPanel, BorderLayout.CENTER);
        add(capturedBlackPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void handleResign() {
        Object[] options = {"Yeni Oyun", "Lobiye Dön", "İptal"};
        int result = JOptionPane.showOptionDialog(
                this,
                "Oyunu bırakmak istiyor musunuz?\nYeni oyun mu oynamak istersiniz yoksa lobiye mi dönmek?",
                "Oyunu Bırak",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (result == JOptionPane.YES_OPTION) {
            client.sendResignWithReplay(); // Sunucuya bildir, diğer oyuncuya da sor
        } else if (result == JOptionPane.NO_OPTION) {
            client.sendResignToLobby(); // sunucuya bildir
            client.showLobby();         // LOBİYE DÖN
        }

    }

    private void setupBoard(JPanel boardPanel) {
        boolean isWhite = false;
        for (int row = 0; row < 8; row++) {
            isWhite = !isWhite;
            for (int col = 0; col < 8; col++) {
                JButton square = new JButton();
                square.setPreferredSize(new Dimension(60, 60));
                square.setBackground(isWhite ? Color.WHITE : Color.GRAY);
                square.setOpaque(true);
                square.setBorderPainted(false);
                int r = row, c = col;
                square.addActionListener(e -> handleClick(r, c));

                if (row == 1) {
                    square.setIcon(loadIcon("bp.png"));
                }
                if (row == 6) {
                    square.setIcon(loadIcon("wp.png"));
                }
                if (row == 0 || row == 7) {
                    boolean white = row == 7;
                    String prefix = white ? "w" : "b";
                    if (col == 0 || col == 7) {
                        square.setIcon(loadIcon(prefix + "r.png"));
                    }
                    if (col == 1 || col == 6) {
                        square.setIcon(loadIcon(prefix + "n.png"));
                    }
                    if (col == 2 || col == 5) {
                        square.setIcon(loadIcon(prefix + "b.png"));
                    }
                    if (col == 3) {
                        square.setIcon(loadIcon(prefix + "q.png"));
                    }
                    if (col == 4) {
                        square.setIcon(loadIcon(prefix + "k.png"));
                    }
                }

                squares[row][col] = square;
                boardPanel.add(square);
                isWhite = !isWhite;
            }
        }
    }

    // Oyuncuya özel flagler:
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteRookMovedLeft = false;
    private boolean whiteRookMovedRight = false;
    private boolean blackRookMovedLeft = false;
    private boolean blackRookMovedRight = false;
    private int[] enPassantTarget = null; // geçici olarak tutulur {row, col}

    private boolean isKingUnderThreat(int row, int col) {
        Icon piece = squares[row][col].getIcon();
        if (piece == null || !((ImageIcon) piece).getDescription().endsWith("k.png")) {
            return false;
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Icon targetPiece = squares[r][c].getIcon();
                if (targetPiece != null) {
                    String targetDesc = ((ImageIcon) targetPiece).getDescription();
                    if (targetDesc.charAt(0) != piece.toString().charAt(0)) {
                        if (isValidMove(targetDesc, r, c, row, col)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isCastlingMoveValid(int fromRow, int fromCol, int toRow, int toCol, String pieceName) {
        if (!pieceName.endsWith("k.png")) {
            return false;
        }
        if (fromRow != toRow || Math.abs(toCol - fromCol) != 2) {
            return false;
        }

        boolean isWhite = pieceName.startsWith("w");

        if (isWhite) {
            if (whiteKingMoved) {
                return false;
            }
            if (toCol == 6 && whiteRookMovedRight) {
                return false; // küçük rok
            }
            if (toCol == 2 && whiteRookMovedLeft) {
                return false;  // büyük rok
            }
        } else {
            if (blackKingMoved) {
                return false;
            }
            if (toCol == 6 && blackRookMovedRight) {
                return false;
            }
            if (toCol == 2 && blackRookMovedLeft) {
                return false;
            }
        }

        // Aradaki taşlar boş mu
        int step = (toCol > fromCol) ? 1 : -1;
        for (int c = fromCol + step; c != toCol; c += step) {
            if (squares[fromRow][c].getIcon() != null) {
                return false;
            }
        }

        // Rok sırasında şah tehdit altında olmamalı
        if (isKingUnderThreat(fromRow, fromCol)) {
            return false;
        }
        if (isKingUnderThreat(fromRow, fromCol + step)) {
            return false;
        }
        if (isKingUnderThreat(toRow, toCol)) {
            return false;
        }

        return true;
    }

    private void handleCastling(int fromRow, int fromCol, int toRow, int toCol) {
        Icon kingIcon = squares[fromRow][fromCol].getIcon();
        String pieceName = ((ImageIcon) kingIcon).getDescription();
        boolean isWhite = pieceName.startsWith("w");

        // Kısa rok (sağ)
        if (toCol == fromCol + 2) {
            Icon rookIcon = squares[fromRow][7].getIcon(); // h1 / h8
            squares[fromRow][5].setIcon(rookIcon);         // f1 / f8
            squares[fromRow][7].setIcon(null);
            squares[fromRow][fromCol].setIcon(null);
            squares[fromRow][6].setIcon(kingIcon);

            // Flag güncelle
            if (isWhite) {
                whiteKingMoved = true;
                whiteRookMovedRight = true;
            } else {
                blackKingMoved = true;
                blackRookMovedRight = true;
            }

        } // Uzun rok (sol)
        else if (toCol == fromCol - 2) {
            Icon rookIcon = squares[fromRow][0].getIcon(); // a1 / a8
            squares[fromRow][3].setIcon(rookIcon);         // d1 / d8
            squares[fromRow][0].setIcon(null);
            squares[fromRow][fromCol].setIcon(null);
            squares[fromRow][2].setIcon(kingIcon);

            // Flag güncelle
            if (isWhite) {
                whiteKingMoved = true;
                whiteRookMovedLeft = true;
            } else {
                blackKingMoved = true;
                blackRookMovedLeft = true;
            }
        }
    }

    private boolean isEnPassantValid(int fromRow, int fromCol, int toRow, int toCol, String pieceName) {
        int direction = pieceName.startsWith("w") ? -1 : 1;  // Beyaz için -1, Siyah için 1

        // Eğer piyon 2 kare ilerleyip rakip piyonun yanına gelmişse, En Passant yapılabilir
        if (Math.abs(toCol - fromCol) == 1 && toRow == fromRow + (2 * direction)) {
            Icon target = squares[toRow][toCol].getIcon();
            if (target == null) {
                return false;  // Eğer hedef karede taş yoksa, En Passant geçerli olamaz
            }

            String targetDesc = ((ImageIcon) target).getDescription();
            if (targetDesc.endsWith("p.png") && targetDesc.charAt(0) != pieceName.charAt(0)) {
                // Eğer rakip piyon yanındaki karede bulunuyorsa, En Passant yapılabilir
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyValidMove(boolean white) {
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                Icon piece = squares[fromRow][fromCol].getIcon();
                if (piece == null) {
                    continue;
                }

                String name = ((ImageIcon) piece).getDescription();
                if ((white && !name.startsWith("w")) || (!white && !name.startsWith("b"))) {
                    continue;
                }

                for (int toRow = 0; toRow < 8; toRow++) {
                    for (int toCol = 0; toCol < 8; toCol++) {
                        if (fromRow == toRow && fromCol == toCol) {
                            continue;
                        }

                        if (!isValidMove(name, fromRow, fromCol, toRow, toCol)) {
                            continue;
                        }

                        // Hamleyi simüle et
                        Icon captured = squares[toRow][toCol].getIcon();
                        squares[toRow][toCol].setIcon(piece);
                        squares[fromRow][fromCol].setIcon(null);

                        boolean kingInCheck = isKingInCheck(white);

                        // Geri al
                        squares[fromRow][fromCol].setIcon(piece);
                        squares[toRow][toCol].setIcon(captured);

                        if (!kingInCheck) {
                            return true; // Bu hamle şahı kurtarıyor
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isKingInCheck(boolean white) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Icon icon = squares[row][col].getIcon();
                if (icon != null) {
                    String name = ((ImageIcon) icon).getDescription();
                    if ((white && name.equals("wk.png")) || (!white && name.equals("bk.png"))) {
                        return isKingUnderThreat(row, col);
                    }
                }
            }
        }
        return false;
    }

    private void handleClick(int row, int col) {
        System.out.println("👉 Tıklanan kare: " + row + "," + col);
        System.out.println("🔁 playerId = " + client.getPlayerId() + ", turn = " + client.getCurrentTurn());

        if (!client.isConnected()) {
            System.out.println("🚫 Client bağlı değil!");
            return;
        }

        if (client.getPlayerId() != client.getCurrentTurn()) {
            System.out.println("🚫 Sıra sende değil!");
            return;
        }

        JButton clicked = squares[row][col];
        Icon icon = clicked.getIcon();

        if (selected == null) {
            if (icon != null) {
                String iconDesc = ((ImageIcon) icon).getDescription();
                boolean isWhite = iconDesc.startsWith("w");
                boolean isBlack = iconDesc.startsWith("b");

                if ((client.getPlayerId() == 1 && isWhite) || (client.getPlayerId() == 0 && isBlack)) {
                    selected = clicked;
                    selectedRow = row;
                    selectedCol = col;
                    clicked.setBackground(Color.PINK);
                    clicked.setBorder(BorderFactory.createLineBorder(Color.PINK, 3));
                    System.out.println("✅ Taş seçildi: " + iconDesc);
                } else {
                    System.out.println("🚫 Bu taş sana ait değil: " + iconDesc);
                    client.log("❌ Sadece kendi taşınızı oynatabilirsiniz!");
                }
            } else {
                System.out.println("⚠️ Boş kareye tıklandı, seçim iptal.");
            }
        } else {
            selected.setBackground(originalColor(selectedRow, selectedCol));
            selected.setBorder(null);

            Icon piece = selected.getIcon();
            if (piece == null) {
                System.out.println("❌ Hata: Seçilen karede taş yok!");
                selected = null;
                return;
            }

            String pieceName = ((ImageIcon) piece).getDescription();

            // Rok kontrolü
            if (pieceName.endsWith("k.png") && isCastlingMoveValid(selectedRow, selectedCol, row, col, pieceName)) {
                client.sendMove(selectedRow, selectedCol, row, col, null, true);
                client.setCurrentTurn(1 - client.getCurrentTurn()); // 🧠 TURU GÜNCELLE
                selected = null;
                return;
            }

            // Şah tehdit altındaysa kontrol
            boolean currentPlayerWhite = client.getPlayerId() == 1;
            if (isKingInCheck(currentPlayerWhite)) {
                Icon originalTarget = squares[row][col].getIcon();
                Icon pieceIcon = squares[selectedRow][selectedCol].getIcon();

                squares[row][col].setIcon(pieceIcon);
                squares[selectedRow][selectedCol].setIcon(null);

                boolean stillInCheck = isKingInCheck(currentPlayerWhite);

                squares[selectedRow][selectedCol].setIcon(pieceIcon);
                squares[row][col].setIcon(originalTarget);

                if (stillInCheck) {
                    client.log("❌ Şah tehdit altında, sadece şahı kurtaran hamlelere izin verilir!");
                    selected = null;
                    return;
                }
            }

            if (!isValidMove(pieceName, selectedRow, selectedCol, row, col)) {
                System.out.println("❌ Geçersiz hamle!");
                client.log("❌ Bu hamleye izin verilmiyor!");
                selected = null;
                return;
            }

            if (pieceName.endsWith("p.png") && (row == 0 || row == 7)) {
                String newPiece = showPromotionDialog(pieceName.startsWith("w") ? "white" : "black");
                if (newPiece != null) {
                    selected.setIcon(loadIcon(newPiece));
                    client.sendMove(selectedRow, selectedCol, row, col, newPiece, false);
                    client.setCurrentTurn(1 - client.getCurrentTurn()); // 🧠 TURU GÜNCELLE
                }
            } else {
                client.sendMove(selectedRow, selectedCol, row, col, null, false);
                client.setCurrentTurn(1 - client.getCurrentTurn()); // 🧠 TURU GÜNCELLE
            }

            selected = null;
        }
    }

// Taş terfisi için seçim ekranı
    private String showPromotionDialog(String color) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(this,
                "Taşınızı Seçin:",
                "Taş Terfisi",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        String prefix = color.equals("white") ? "w" : "b";

        if (choice == 0) {
            return prefix + "q.png";  // Vezir
        }
        if (choice == 1) {
            return prefix + "r.png";  // Kale
        }
        if (choice == 2) {
            return prefix + "b.png";  // Fil
        }
        if (choice == 3) {
            return prefix + "n.png";  // At
        }
        return null;
    }

    private boolean isValidMove(String pieceName, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow < 0 || fromRow >= 8 || fromCol < 0 || fromCol >= 8
                || toRow < 0 || toRow >= 8 || toCol < 0 || toCol >= 8) {
            return false;
        }

        int direction = pieceName.startsWith("w") ? -1 : 1;

        // ♟ Piyon hareketleri
        if (pieceName.endsWith("p.png")) {
            // Düz ilerleme
            if (fromCol == toCol && squares[toRow][toCol].getIcon() == null) {
                if (toRow - fromRow == direction) {
                    return true;
                }
                if ((pieceName.startsWith("w") && fromRow == 6 || pieceName.startsWith("b") && fromRow == 1)
                        && toRow - fromRow == 2 * direction && squares[fromRow + direction][fromCol].getIcon() == null) {
                    return true;
                }
            }

            // Normal çapraz alma
            if (Math.abs(toCol - fromCol) == 1 && toRow - fromRow == direction) {
                Icon target = squares[toRow][toCol].getIcon();
                if (target != null) {
                    String targetDesc = ((ImageIcon) target).getDescription();
                    return pieceName.charAt(0) != targetDesc.charAt(0);
                }

                // ♟ En passant çapraz alma
                if (enPassantTarget != null && enPassantTarget[0] == toRow && enPassantTarget[1] == toCol) {
                    return true;
                }
            }

            return false;
        }

        // ♜ Kale hareketleri
        if (pieceName.endsWith("r.png")) {
            if (fromRow == toRow) {
                int step = fromCol < toCol ? 1 : -1;
                for (int c = fromCol + step; c != toCol; c += step) {
                    if (squares[fromRow][c].getIcon() != null) {
                        return false;
                    }
                }
                return isCaptureLegal(pieceName, toRow, toCol);
            } else if (fromCol == toCol) {
                int step = fromRow < toRow ? 1 : -1;
                for (int r = fromRow + step; r != toRow; r += step) {
                    if (squares[r][fromCol].getIcon() != null) {
                        return false;
                    }
                }
                return isCaptureLegal(pieceName, toRow, toCol);
            }
            return false;
        }

        // ♝ Fil
        if (pieceName.endsWith("b.png")) {
            if (Math.abs(toRow - fromRow) == Math.abs(toCol - fromCol)) {
                int rowStep = (toRow - fromRow) > 0 ? 1 : -1;
                int colStep = (toCol - fromCol) > 0 ? 1 : -1;

                int r = fromRow + rowStep;
                int c = fromCol + colStep;
                while (r != toRow && c != toCol) {
                    if (squares[r][c].getIcon() != null) {
                        return false;
                    }
                    r += rowStep;
                    c += colStep;
                }
                return isCaptureLegal(pieceName, toRow, toCol);
            }
            return false;
        }

        // ♞ At
        if (pieceName.endsWith("n.png")) {
            int rowDiff = Math.abs(toRow - fromRow);
            int colDiff = Math.abs(toCol - fromCol);
            if ((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)) {
                return isCaptureLegal(pieceName, toRow, toCol);
            }
            return false;
        }

        // ♛ Vezir
        if (pieceName.endsWith("q.png")) {
            int rowDiff = Math.abs(toRow - fromRow);
            int colDiff = Math.abs(toCol - fromCol);

            if (rowDiff == colDiff || fromRow == toRow || fromCol == toCol) {
                return isPathClear(fromRow, fromCol, toRow, toCol)
                        && isCaptureLegal(pieceName, toRow, toCol);
            }
            return false;
        }

        // ♚ Şah
        if (pieceName.endsWith("k.png")) {
            int rowDiff = Math.abs(toRow - fromRow);
            int colDiff = Math.abs(toCol - fromCol);

            if (rowDiff <= 1 && colDiff <= 1) {
                return isCaptureLegal(pieceName, toRow, toCol);
            }
            return false;
        }

        return true;
    }

    private boolean isCaptureLegal(String pieceName, int toRow, int toCol) {
        Icon target = squares[toRow][toCol].getIcon();
        if (target == null) {
            return true;
        }
        String targetDesc = ((ImageIcon) target).getDescription();
        return pieceName.charAt(0) != targetDesc.charAt(0);
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int r = fromRow + rowStep;
        int c = fromCol + colStep;

        while (r != toRow || c != toCol) {
            if (squares[r][c].getIcon() != null) {
                return false;
            }
            r += rowStep;
            c += colStep;
        }
        return true;
    }

    public void movePiece(int fromRow, int fromCol, int toRow, int toCol, String promotedPiece, boolean isCastling) {
        Icon piece = squares[fromRow][fromCol].getIcon();
        String pieceName = ((ImageIcon) piece).getDescription();

        // ♟ Rok işlemi
        if (isCastling) {
            handleCastling(fromRow, fromCol, toRow, toCol);
            if (pieceName.startsWith("w")) {
                whiteKingMoved = true;
                if (toCol == fromCol + 2) {
                    whiteRookMovedRight = true;
                }
                if (toCol == fromCol - 2) {
                    whiteRookMovedLeft = true;
                }
            } else {
                blackKingMoved = true;
                if (toCol == fromCol + 2) {
                    blackRookMovedRight = true;
                }
                if (toCol == fromCol - 2) {
                    blackRookMovedLeft = true;
                }
            }
            enPassantTarget = null; // Rok sırasında en passant hakkı olmaz
            return;
        }

        // ♟ En passant taşını alma
        if (pieceName.endsWith("p.png")
                && Math.abs(toCol - fromCol) == 1
                && squares[toRow][toCol].getIcon() == null
                && enPassantTarget != null
                && enPassantTarget[0] == toRow && enPassantTarget[1] == toCol) {

            int capturedRow = pieceName.startsWith("w") ? toRow + 1 : toRow - 1;
            Icon captured = squares[capturedRow][toCol].getIcon();
            if (captured != null) {
                String desc = ((ImageIcon) captured).getDescription();
                if (desc.contains("w")) {
                    capturedWhitePanel.add(new JLabel(captured));
                } else {
                    capturedBlackPanel.add(new JLabel(captured));
                }
            }
            squares[capturedRow][toCol].setIcon(null); // rakip piyonu kaldır
        }

        // ♟ Rakip taş varsa normal yakalama
        Icon target = squares[toRow][toCol].getIcon();
        if (target != null) {
            String desc = ((ImageIcon) target).getDescription();
            if (desc.contains("w")) {
                capturedWhitePanel.add(new JLabel(target));
            } else {
                capturedBlackPanel.add(new JLabel(target));
            }
        }

        capturedWhitePanel.revalidate();
        capturedBlackPanel.revalidate();

        // ♟ Terfi varsa taşı değiştir
        if (promotedPiece != null) {
            squares[fromRow][fromCol].setIcon(loadIcon(promotedPiece));
            piece = squares[fromRow][fromCol].getIcon(); // güncelle
        }

        // ♟ Taşı hareket ettir
        squares[fromRow][fromCol].setIcon(null);
        squares[toRow][toCol].setIcon(piece);

        // ♟ En passant hakkı: sadece piyon 2 kare ilerlediyse açılır
        if (pieceName.endsWith("p.png") && Math.abs(toRow - fromRow) == 2) {
            enPassantTarget = new int[]{(fromRow + toRow) / 2, fromCol}; // geçilen kare
        } else {
            enPassantTarget = null;
        }

        // ♟ Rok dışı kale/şah flag güncelle
        if (pieceName.endsWith("k.png")) {
            if (pieceName.startsWith("w")) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        }

        if (pieceName.endsWith("r.png")) {
            if (pieceName.startsWith("w")) {
                if (fromCol == 0) {
                    whiteRookMovedLeft = true;
                }
                if (fromCol == 7) {
                    whiteRookMovedRight = true;
                }
            } else {
                if (fromCol == 0) {
                    blackRookMovedLeft = true;
                }
                if (fromCol == 7) {
                    blackRookMovedRight = true;
                }
            }
        }

        // ♟ OYUN BİTİŞİ KONTROLÜ – Hamle sonrası kontrol
        boolean whiteToMove = client.getCurrentTurn() == 1;
        if (!hasAnyValidMove(whiteToMove)) {
            if (isKingInCheck(whiteToMove)) {
                client.log("♚ ŞAH MAT! " + (whiteToMove ? "Siyah kazandı." : "Beyaz kazandı."));
                JOptionPane.showMessageDialog(this, "♚ ŞAH MAT!\n" + (whiteToMove ? "Siyah kazandı." : "Beyaz kazandı."));
            } else {
                client.log("🤝 PAT! Oyun berabere.");
                JOptionPane.showMessageDialog(this, "🤝 PAT!\nOyun berabere.");
            }
        }

        System.out.println("movePiece: " + fromRow + "," + fromCol + " -> " + toRow + "," + toCol);
    }

    private Color originalColor(int row, int col) {
        return (row + col) % 2 == 0 ? Color.WHITE : Color.GRAY;
    }

    private ImageIcon loadIcon(String fileName) {
        ImageIcon icon = new ImageIcon(getClass().getResource("/pieces/" + fileName));
        icon.setDescription(fileName);
        Image scaled = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled) {
            {
                setDescription(fileName);
            }
        };
    }

    public void disableInput() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setEnabled(false);
            }
        }
    }

}
