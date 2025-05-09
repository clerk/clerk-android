echo "Installing pre commit hooks"
if [ ! -f .git/hooks/pre-commit ]; then
    touch ./.git/hooks/
fi
cp ./config/git/hooks/pre-commit ./.git/hooks/pre-commit && chmod +x ./.git/hooks/pre-commit